-- SIGA / TINGESO — Oferta académica de prueba para 2026-2.
-- Basado en Entrega-1 (Copiar)(9).zip y siga_seed_oficial.sql.
-- Crea UNA sección ACTIVA por cada una de las 72 asignaturas oficiales (si falta).
-- NO crea inscripciones ni cambia cupos, notas o secciones existentes.
-- Agrega hasta 12 docentes ficticios de prueba SIN cuentas Keycloak, para evitar topes.
-- Ejecución manual, EXCLUSIVAMENTE en una base de datos de desarrollo.
-- Hacer respaldo de PostgreSQL antes de aplicar. No agregar a un init automático.
-- Si algo falla, toda la transacción se revierte.

BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '90s';

-- Protección compartida con el actualizador de horarios; no ejecutar con escrituras de la aplicación.
SELECT pg_advisory_xact_lock(2026, 22026);
-- Evitar cambios simultáneos de oferta, docentes e inscripciones durante la planificación.
LOCK TABLE academic_periods, sections, section_blocks, enrollments, grades, teachers, subjects, study_plans, careers
    IN SHARE ROW EXCLUSIVE MODE;

-- 1) Docentes ficticios de desarrollo. Se conservan los existentes.
-- "SEED-2026-XX" es un identificador de prueba, NO un RUT real.
INSERT INTO teachers
    (rut, first_name, last_name, email, professional_title, academic_degree, status)
SELECT
    'SEED-2026-' || lpad(n::text, 2, '0'),
    'Docente',
    'Prueba ' || lpad(n::text, 2, '0'),
    'docente-seed-' || lpad(n::text, 2, '0') || '@example.invalid',
    'Docente de prueba',
    'Grado de prueba',
    'ACTIVA'
FROM generate_series(1, 12) AS n
WHERE NOT EXISTS (
    SELECT 1
    FROM teachers t
    WHERE t.rut = 'SEED-2026-' || lpad(n::text, 2, '0')
       OR t.email = 'docente-seed-' || lpad(n::text, 2, '0') || '@example.invalid'
);

-- 2) Validaciones iniciales + programación de secciones y bloques.
DO $seed$
DECLARE
    v_period_id        BIGINT;
    v_total_subjects   INTEGER;
    v_total_teachers   INTEGER;
    v_total_sections   INTEGER;
    v_tap101_teacher   BIGINT;
    v_new_section_id   BIGINT;
    v_teacher_id       BIGINT;
    v_needed           INTEGER;
    v_slots            TEXT[];
    v_preferred        TEXT[];
    v_selected         TEXT[];
    v_created          INTEGER := 0;
    r                   RECORD;
    candidate           RECORD;
BEGIN
    SELECT ap.id INTO v_period_id
    FROM academic_periods ap
    WHERE ap.code = '2026-2' AND ap.status = 'ABIERTO'
    FOR UPDATE;

    IF v_period_id IS NULL THEN
        RAISE EXCEPTION 'No existe el periodo 2026-2 en estado ABIERTO. No se hizo ningun cambio';
    END IF;

    -- Validar las 24 asignaturas de cada una de las tres carreras oficiales.
    SELECT COUNT(*) INTO v_total_subjects
    FROM subjects s
    JOIN study_plans p ON p.id = s.study_plan_id
    JOIN careers c ON c.id = p.career_id
    WHERE (c.code = '10450' AND p.identifier = '2021.3' AND s.code ~ '^TAP[1-4]0[1-6]$')
       OR (c.code = '10451' AND p.identifier = '2022.3' AND s.code ~ '^TAD[1-4]0[1-6]$')
       OR (c.code = '10452' AND p.identifier = '2021.3' AND s.code ~ '^TCS[1-4]0[1-6]$');

    IF v_total_subjects <> 72 THEN
        RAISE EXCEPTION 'Se esperaban 72 asignaturas oficiales, pero se encontraron %. Carga primero el seed oficial', v_total_subjects;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (VALUES ('10450','2021.3','TAP'), ('10451','2022.3','TAD'),
                     ('10452','2021.3','TCS')) AS expected(career_code, plan_identifier, prefix)
        CROSS JOIN generate_series(1, 4) AS semester(number)
        CROSS JOIN generate_series(1, 6) AS subject_number(number)
        WHERE (SELECT COUNT(*) FROM subjects s
               JOIN study_plans p ON p.id = s.study_plan_id
               JOIN careers c ON c.id = p.career_id
               WHERE c.code = expected.career_code AND p.identifier = expected.plan_identifier
                 AND s.semester = semester.number
                 AND s.code = expected.prefix || semester.number::text || '0' || subject_number.number::text) <> 1
    ) THEN
        RAISE EXCEPTION 'La malla oficial no contiene exactamente las 72 asignaturas esperadas, distribuidas en cuatro semestres';
    END IF;

    SELECT COUNT(*) INTO v_total_teachers
    FROM teachers
    WHERE rut ~ '^SEED-2026-(0[1-9]|1[0-2])$'
      AND email ~ '^docente-seed-(0[1-9]|1[0-2])@example[.]invalid$'
      AND status = 'ACTIVA';

    IF v_total_teachers <> 12 THEN
        RAISE EXCEPTION 'Se esperaban 12 docentes de prueba activos, pero hay %. Revisar colisiones de RUT/email o cambios de estado', v_total_teachers;
    END IF;

    -- Cuando ya existe TAP101, preferimos su mismo docente para el primer semestre TAP;
    -- así también sirve para probar la vista TEACHER con la cuenta docente existente.
    SELECT sec.teacher_id INTO v_tap101_teacher
    FROM sections sec
    JOIN subjects s ON s.id = sec.subject_id
    JOIN study_plans p ON p.id = s.study_plan_id
    JOIN careers c ON c.id = p.career_id
    WHERE sec.academic_period_id = v_period_id
      AND c.code = '10450'
      AND p.identifier = '2021.3'
      AND s.code = 'TAP101'
    LIMIT 1;

    FOR r IN
        SELECT s.id AS subject_id, s.code, s.semester, s.study_plan_id,
               c.code AS career_code,
               s.theory_hours + s.exercise_hours + s.laboratory_hours AS total_hours
        FROM subjects s
        JOIN study_plans p ON p.id = s.study_plan_id
        JOIN careers c ON c.id = p.career_id
        WHERE (c.code = '10450' AND p.identifier = '2021.3' AND s.code ~ '^TAP[1-4]0[1-6]$')
           OR (c.code = '10451' AND p.identifier = '2022.3' AND s.code ~ '^TAD[1-4]0[1-6]$')
           OR (c.code = '10452' AND p.identifier = '2021.3' AND s.code ~ '^TCS[1-4]0[1-6]$')
        ORDER BY c.code, s.semester, s.code
    LOOP
        -- Respetar toda sección existente: docente, bloques, capacidad, inscripciones, etc.
        IF EXISTS (
            SELECT 1 FROM sections sec
            WHERE sec.subject_id = r.subject_id
              AND sec.academic_period_id = v_period_id
        ) THEN
            CONTINUE;
        END IF;

        IF r.total_hours IS NULL OR r.total_hours <= 0
           OR r.total_hours % 2 <> 0 OR r.total_hours > 12 THEN
            RAISE EXCEPTION 'Horas TEL invalidas para %: %', r.code, r.total_hours;
        END IF;
        v_needed := r.total_hours / 2;

        -- Permutación completa de los 30 bloques, distinta por carrera, semestre y asignatura.
        -- En cada grupo de cinco alternativas cambia el día; en los siguientes rota el módulo.
        SELECT array_agg(
            (ARRAY['L','M','W','J','V'])[1 + mod(slot.position + right(r.code, 1)::int * 2 +
                r.semester + (r.career_code::int - 10450), 5)] ||
            (1 + mod(slot.position / 5 + mod(slot.position, 5) * 2 + right(r.code, 1)::int +
                r.semester + (r.career_code::int - 10450), 6))::text
            ORDER BY slot.position)
        INTO v_slots
        FROM generate_series(0, 29) AS slot(position);

        -- Preferencias compatibles para las seis asignaturas iniciales de TAP.
        -- TAP101 existente se conserva siempre (el CONTINUE anterior no modifica sus bloques).
        v_preferred := CASE r.code
            WHEN 'TAP101' THEN ARRAY['L1','W1','V1']
            WHEN 'TAP102' THEN ARRAY['M2','J4']
            WHEN 'TAP103' THEN ARRAY['L3','V5']
            WHEN 'TAP104' THEN ARRAY['W2','M6']
            WHEN 'TAP105' THEN ARRAY['J1','L5']
            WHEN 'TAP106' THEN ARRAY['V3','W6']
            ELSE ARRAY[]::TEXT[]
        END;

        v_teacher_id := NULL;
        v_selected := ARRAY[]::TEXT[];

        -- Primero se intenta con el docente de TAP101 (solo semestre 1 TAP).
        -- Para el resto, distribuir el trabajo entre los 12 docentes de prueba.
        FOR candidate IN
            SELECT t.id
            FROM teachers t
            WHERE t.status = 'ACTIVA'
              AND (
                   (t.rut ~ '^SEED-2026-(0[1-9]|1[0-2])$'
                    AND t.email ~ '^docente-seed-(0[1-9]|1[0-2])@example[.]invalid$')
                   OR (r.career_code = '10450' AND r.semester = 1
                       AND v_tap101_teacher IS NOT NULL AND t.id = v_tap101_teacher)
              )
            ORDER BY
                CASE WHEN r.career_code = '10450' AND r.semester = 1
                          AND t.id = v_tap101_teacher THEN 0 ELSE 1 END,
                (SELECT COUNT(*)
                 FROM sections sec
                 JOIN section_blocks sb ON sb.section_id = sec.id
                 WHERE sec.teacher_id = t.id
                   AND sec.academic_period_id = v_period_id),
                t.id
        LOOP
            -- Tomar N bloques válidos que:
            --   a) no choquen con ninguna sección del MISMO docente en 2026-2,
            --   b) no choquen con ninguna asignatura del MISMO plan y semestre.
            -- Esto permite inscribir juntas las seis asignaturas de un semestre.
            SELECT ARRAY(
                SELECT slot.block_code
                FROM unnest(v_slots) WITH ORDINALITY AS slot(block_code, position)
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM section_blocks sb
                    JOIN sections sec ON sec.id = sb.section_id
                    WHERE sec.academic_period_id = v_period_id
                      AND sec.teacher_id = candidate.id
                      AND sb.blocks = slot.block_code
                )
                AND NOT EXISTS (
                    SELECT 1
                    FROM section_blocks sb
                    JOIN sections sec ON sec.id = sb.section_id
                    JOIN subjects other_s ON other_s.id = sec.subject_id
                    WHERE sec.academic_period_id = v_period_id
                      AND other_s.study_plan_id = r.study_plan_id
                      AND other_s.semester = r.semester
                      AND sb.blocks = slot.block_code
                )
                ORDER BY
                    CASE WHEN slot.block_code = ANY(v_preferred) THEN 0 ELSE 1 END,
                    COALESCE(array_position(v_preferred, slot.block_code), 1000),
                    slot.position
                LIMIT v_needed
            ) INTO v_selected;

            IF cardinality(v_selected) = v_needed THEN
                v_teacher_id := candidate.id;
                EXIT;
            END IF;
        END LOOP;

        IF v_teacher_id IS NULL THEN
            RAISE EXCEPTION 'No se encontraron % bloques compatibles para % (periodo 2026-2). No se guardo nada',
                v_needed, r.code;
        END IF;

        INSERT INTO sections
            (subject_id, academic_period_id, teacher_id,
             capacity, enrolled_students, status, version)
        VALUES
            (r.subject_id, v_period_id, v_teacher_id,
             30, 0, 'ACTIVA', 0)
        RETURNING id INTO v_new_section_id;

        INSERT INTO section_blocks (section_id, blocks)
        SELECT v_new_section_id, block_code
        FROM unnest(v_selected) AS x(block_code);

        IF r.code = 'TAP101' THEN
            v_tap101_teacher := v_teacher_id;
        END IF;
        v_created := v_created + 1;
    END LOOP;

    -- Verificar que se ofrece exactamente una sección por asignatura oficial.
    SELECT COUNT(*) INTO v_total_sections
    FROM sections sec
    JOIN subjects s ON s.id = sec.subject_id
    JOIN study_plans p ON p.id = s.study_plan_id
    JOIN careers c ON c.id = p.career_id
    WHERE sec.academic_period_id = v_period_id
      AND ((c.code = '10450' AND p.identifier = '2021.3' AND s.code ~ '^TAP[1-4]0[1-6]$')
        OR (c.code = '10451' AND p.identifier = '2022.3' AND s.code ~ '^TAD[1-4]0[1-6]$')
        OR (c.code = '10452' AND p.identifier = '2021.3' AND s.code ~ '^TCS[1-4]0[1-6]$'));

    IF v_total_sections <> 72 THEN
        RAISE EXCEPTION 'Resultado inesperado: % secciones en vez de 72. Transaccion revertida', v_total_sections;
    END IF;

    -- Comprobar la oferta completa, incluidas las secciones que se conservaron.
    IF EXISTS (
        SELECT 1 FROM sections sec JOIN section_blocks sb ON sb.section_id = sec.id
        WHERE sec.academic_period_id = v_period_id
        GROUP BY sec.teacher_id, sb.blocks HAVING COUNT(DISTINCT sec.id) > 1
    ) THEN
        RAISE EXCEPTION 'La oferta mantiene un tope docente en 2026-2. No se guardo nada';
    END IF;
    IF EXISTS (
        SELECT 1 FROM sections sec
        JOIN subjects s ON s.id = sec.subject_id
        JOIN study_plans p ON p.id = s.study_plan_id
        JOIN careers c ON c.id = p.career_id
        WHERE sec.academic_period_id = v_period_id
          AND ((c.code = '10450' AND p.identifier = '2021.3')
            OR (c.code = '10451' AND p.identifier = '2022.3')
            OR (c.code = '10452' AND p.identifier = '2021.3'))
          AND (s.theory_hours + s.exercise_hours + s.laboratory_hours IS NULL
               OR s.theory_hours + s.exercise_hours + s.laboratory_hours <= 0
               OR (s.theory_hours + s.exercise_hours + s.laboratory_hours) % 2 <> 0
               OR (SELECT COUNT(*) * 2 FROM section_blocks sb WHERE sb.section_id = sec.id)
                    <> s.theory_hours + s.exercise_hours + s.laboratory_hours
               OR EXISTS (SELECT 1 FROM section_blocks sb WHERE sb.section_id = sec.id
                          AND (sb.blocks IS NULL OR sb.blocks !~ '^[LMWJV][1-6]$'))
               OR EXISTS (SELECT 1 FROM section_blocks sb WHERE sb.section_id = sec.id
                          GROUP BY sb.blocks HAVING COUNT(*) > 1))
    ) THEN
        RAISE EXCEPTION 'Hay una seccion con bloques invalidos o incompatibles con TEL. No se guardo nada';
    END IF;
    IF EXISTS (
        SELECT 1 FROM sections sec
        JOIN subjects s ON s.id = sec.subject_id
        JOIN study_plans p ON p.id = s.study_plan_id
        JOIN careers c ON c.id = p.career_id
        JOIN section_blocks sb ON sb.section_id = sec.id
        WHERE sec.academic_period_id = v_period_id
          AND ((c.code = '10450' AND p.identifier = '2021.3')
            OR (c.code = '10451' AND p.identifier = '2022.3')
            OR (c.code = '10452' AND p.identifier = '2021.3'))
        GROUP BY s.study_plan_id, s.semester, sb.blocks HAVING COUNT(DISTINCT sec.id) > 1
    ) THEN
        RAISE EXCEPTION 'Hay un semestre con topes entre secciones protegidas. No se guardo nada';
    END IF;

    RAISE NOTICE 'SIGA: se crearon % secciones nuevas. Oferta oficial del periodo 2026-2: % secciones.',
        v_created, v_total_sections;
END;
$seed$;

COMMIT;

-- Solo consulta: resumen por carrera y semestre (72 secciones esperadas en total).
SELECT c.code AS carrera, s.semester AS semestre, COUNT(*) AS secciones
FROM sections sec
JOIN academic_periods ap ON ap.id = sec.academic_period_id
JOIN subjects s ON s.id = sec.subject_id
JOIN study_plans p ON p.id = s.study_plan_id
JOIN careers c ON c.id = p.career_id
WHERE ap.code = '2026-2'
  AND ((c.code = '10450' AND p.identifier = '2021.3' AND s.code ~ '^TAP[1-4]0[1-6]$')
    OR (c.code = '10451' AND p.identifier = '2022.3' AND s.code ~ '^TAD[1-4]0[1-6]$')
    OR (c.code = '10452' AND p.identifier = '2021.3' AND s.code ~ '^TCS[1-4]0[1-6]$'))
GROUP BY c.code, s.semester
ORDER BY c.code, s.semester;
