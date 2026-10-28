-- SIGA IPT - carga inicial desde Evaluacion1_v1.pdf, Apéndice A (págs. 12-13).
-- Ejecución manual en PostgreSQL; no activar spring.sql.init.mode=always.
-- Idempotente: no borra ni sobrescribe registros existentes.
-- Antes de ejecutarlo, realizar copia de seguridad de la BD.
BEGIN;

-- 1. Carreras oficiales: duración de 4 semestres; ACTIVA es estado inicial propuesto.
INSERT INTO careers (code, name, description, status, duration) VALUES
    ('10450', 'Técnico Analista Programador', 'Carrera técnica de cuatro semestres del IPT.', 'ACTIVA', 4),
    ('10451', 'Técnico en Análisis de Datos', 'Carrera técnica de cuatro semestres del IPT.', 'ACTIVA', 4),
    ('10452', 'Técnico en Ciberseguridad', 'Carrera técnica de cuatro semestres del IPT.', 'ACTIVA', 4)
ON CONFLICT (code) DO NOTHING;

-- 2. Un plan VIGENTE para cada carrera oficial, solo si falta.
INSERT INTO study_plans (identifier, status, career_id)
SELECT v.identifier, 'VIGENTE', c.id
FROM (VALUES
    ('10450', '2021.3'),
    ('10451', '2022.3'),
    ('10452', '2021.3')
) AS v(career_code, identifier)
JOIN careers c ON c.code = v.career_code
ON CONFLICT (career_id, identifier) DO NOTHING;

-- 3. Las 72 asignaturas: 24 por cada plan.
WITH data (career_code, plan_identifier, semester, code, name, theory_hours, exercise_hours, laboratory_hours, sct) AS (
    VALUES
        ('10450', '2021.3', 1, 'TAP101', 'Fundamentos de Programación', 2, 2, 2, 6),
        ('10450', '2021.3', 1, 'TAP102', 'Matemática Aplicada', 2, 2, 0, 5),
        ('10450', '2021.3', 1, 'TAP103', 'Fundamentos de Computación', 2, 0, 2, 4),
        ('10450', '2021.3', 1, 'TAP104', 'Bases de Datos I', 2, 0, 2, 5),
        ('10450', '2021.3', 1, 'TAP105', 'Comunicación Efectiva', 2, 2, 0, 4),
        ('10450', '2021.3', 1, 'TAP106', 'Inglés Técnico I', 2, 2, 0, 4),
        ('10450', '2021.3', 2, 'TAP201', 'Programación Orientada a Objetos', 2, 2, 2, 6),
        ('10450', '2021.3', 2, 'TAP202', 'Estructuras de Datos', 2, 2, 2, 6),
        ('10450', '2021.3', 2, 'TAP203', 'Bases de Datos II', 2, 0, 2, 5),
        ('10450', '2021.3', 2, 'TAP204', 'Desarrollo Web I', 2, 0, 4, 6),
        ('10450', '2021.3', 2, 'TAP205', 'Sistemas Operativos', 2, 0, 2, 4),
        ('10450', '2021.3', 2, 'TAP206', 'Inglés Técnico II', 2, 2, 0, 3),
        ('10450', '2021.3', 3, 'TAP301', 'Desarrollo Web II', 2, 0, 4, 6),
        ('10450', '2021.3', 3, 'TAP302', 'Ingeniería de Software', 2, 2, 0, 5),
        ('10450', '2021.3', 3, 'TAP303', 'Desarrollo de Aplicaciones Móviles', 2, 0, 4, 6),
        ('10450', '2021.3', 3, 'TAP304', 'Administración de Bases de Datos', 2, 0, 2, 5),
        ('10450', '2021.3', 3, 'TAP305', 'Redes y Comunicaciones', 2, 0, 2, 4),
        ('10450', '2021.3', 3, 'TAP306', 'Taller de Programación', 0, 0, 4, 4),
        ('10450', '2021.3', 4, 'TAP401', 'Arquitectura y Desarrollo de Software', 2, 0, 4, 6),
        ('10450', '2021.3', 4, 'TAP402', 'Desarrollo de Aplicaciones Cloud', 2, 0, 4, 6),
        ('10450', '2021.3', 4, 'TAP403', 'Seguridad de Aplicaciones', 2, 0, 2, 4),
        ('10450', '2021.3', 4, 'TAP404', 'Calidad y Pruebas de Software', 2, 0, 2, 5),
        ('10450', '2021.3', 4, 'TAP405', 'Proyecto de Desarrollo de Software', 0, 0, 6, 6),
        ('10450', '2021.3', 4, 'TAP406', 'Emprendimiento y Gestión de Proyectos', 2, 2, 0, 3),
        ('10451', '2022.3', 1, 'TAD101', 'Fundamentos de Programación', 2, 2, 2, 6),
        ('10451', '2022.3', 1, 'TAD102', 'Matemática Aplicada', 2, 2, 0, 5),
        ('10451', '2022.3', 1, 'TAD103', 'Introducción al Análisis de Datos', 2, 0, 2, 5),
        ('10451', '2022.3', 1, 'TAD104', 'Bases de Datos I', 2, 0, 2, 5),
        ('10451', '2022.3', 1, 'TAD105', 'Herramientas Computacionales', 0, 0, 4, 4),
        ('10451', '2022.3', 1, 'TAD106', 'Comunicación Efectiva', 2, 2, 0, 4),
        ('10451', '2022.3', 2, 'TAD201', 'Programación para Análisis de Datos', 2, 0, 4, 6),
        ('10451', '2022.3', 2, 'TAD202', 'Probabilidad y Estadística', 2, 2, 2, 6),
        ('10451', '2022.3', 2, 'TAD203', 'Bases de Datos II', 2, 0, 2, 5),
        ('10451', '2022.3', 2, 'TAD204', 'Preparación y Calidad de Datos', 2, 0, 4, 5),
        ('10451', '2022.3', 2, 'TAD205', 'Visualización de Datos', 2, 0, 2, 5),
        ('10451', '2022.3', 2, 'TAD206', 'Inglés Técnico', 2, 2, 0, 3),
        ('10451', '2022.3', 3, 'TAD301', 'Estadística Aplicada', 2, 2, 2, 6),
        ('10451', '2022.3', 3, 'TAD302', 'Minería de Datos', 2, 0, 4, 6),
        ('10451', '2022.3', 3, 'TAD303', 'Inteligencia de Negocios', 2, 0, 4, 5),
        ('10451', '2022.3', 3, 'TAD304', 'Integración y Procesamiento de Datos', 2, 0, 4, 5),
        ('10451', '2022.3', 3, 'TAD305', 'Visualización Avanzada de Datos', 2, 0, 2, 4),
        ('10451', '2022.3', 3, 'TAD306', 'Ética y Protección de Datos', 2, 2, 0, 4),
        ('10451', '2022.3', 4, 'TAD401', 'Fundamentos de Machine Learning', 2, 0, 4, 6),
        ('10451', '2022.3', 4, 'TAD402', 'Big Data y Procesamiento de Datos', 2, 0, 4, 5),
        ('10451', '2022.3', 4, 'TAD403', 'Analítica de Negocios', 2, 0, 2, 5),
        ('10451', '2022.3', 4, 'TAD404', 'Desarrollo de Soluciones de Datos', 2, 0, 4, 5),
        ('10451', '2022.3', 4, 'TAD405', 'Proyecto de Análisis de Datos', 0, 0, 6, 6),
        ('10451', '2022.3', 4, 'TAD406', 'Emprendimiento y Gestión de Proyectos', 2, 2, 0, 3),
        ('10452', '2021.3', 1, 'TCS101', 'Fundamentos de Programación', 2, 2, 2, 6),
        ('10452', '2021.3', 1, 'TCS102', '', 2, 0, 2, 5),
        ('10452', '2021.3', 1, 'TCS103', 'Redes de Computadores I', 2, 0, 2, 5),
        ('10452', '2021.3', 1, 'TCS104', 'Sistemas Operativos', 2, 0, 2, 5),
        ('10452', '2021.3', 1, 'TCS105', 'Matemática Aplicada', 2, 2, 0, 5),
        ('10452', '2021.3', 1, 'TCS106', 'Comunicación Efectiva', 2, 2, 0, 4),
        ('10452', '2021.3', 2, 'TCS201', 'Programación para Ciberseguridad', 2, 0, 4, 5),
        ('10452', '2021.3', 2, 'TCS202', 'Redes de Computadores II', 2, 0, 4, 6),
        ('10452', '2021.3', 2, 'TCS203', 'Administración de Sistemas Linux', 2, 0, 4, 5),
        ('10452', '2021.3', 2, 'TCS204', 'Fundamentos de Ciberseguridad', 2, 2, 2, 6),
        ('10452', '2021.3', 2, 'TCS205', 'Bases de Datos', 2, 0, 2, 5),
        ('10452', '2021.3', 2, 'TCS206', 'Inglés Técnico', 2, 2, 0, 3),
        ('10452', '2021.3', 3, 'TCS301', 'Seguridad de Redes', 2, 0, 4, 6),
        ('10452', '2021.3', 3, 'TCS302', 'Seguridad de Sistemas Operativos', 2, 0, 4, 5),
        ('10452', '2021.3', 3, 'TCS303', 'Criptografía Aplicada', 2, 2, 2, 5),
        ('10452', '2021.3', 3, 'TCS304', 'Seguridad de Aplicaciones Web', 2, 0, 4, 5),
        ('10452', '2021.3', 3, 'TCS305', 'Monitoreo y Detección de Amenazas', 2, 0, 4, 5),
        ('10452', '2021.3', 3, 'TCS306', 'Legislación y Ética en Ciberseguridad', 2, 2, 0, 4),
        ('10452', '2021.3', 4, 'TCS401', 'Ethical Hacking y Pruebas de Penetración', 2, 0, 4, 6),
        ('10452', '2021.3', 4, 'TCS402', 'Gestión de Incidentes de Ciberseguridad', 2, 0, 4, 5),
        ('10452', '2021.3', 4, 'TCS403', 'Seguridad en Cloud', 2, 0, 4, 5),
        ('10452', '2021.3', 4, 'TCS404', 'Análisis Forense Digital', 2, 0, 4, 5),
        ('10452', '2021.3', 4, 'TCS405', 'Proyecto de Ciberseguridad', 0, 0, 6, 6),
        ('10452', '2021.3', 4, 'TCS406', 'Gestión de Riesgos de Ciberseguridad', 2, 2, 0, 3)
)
INSERT INTO subjects (study_plan_id, semester, code, name, theory_hours, exercise_hours, laboratory_hours, sct)
SELECT p.id, d.semester, d.code, d.name, d.theory_hours, d.exercise_hours, d.laboratory_hours, d.sct
FROM data d
JOIN careers c ON c.code = d.career_code
JOIN study_plans p ON p.career_id = c.id AND p.identifier = d.plan_identifier
ON CONFLICT (study_plan_id, code) DO NOTHING;

-- 4. Prerrequisitos del Apéndice A.
WITH prerequisites (subject_code, prerequisite_code) AS (
    VALUES
        ('TAP201', 'TAP101'),
        ('TAP202', 'TAP101'),
        ('TAP203', 'TAP104'),
        ('TAP204', 'TAP101'),
        ('TAP205', 'TAP103'),
        ('TAP206', 'TAP106'),
        ('TAP301', 'TAP201'),
        ('TAP301', 'TAP204'),
        ('TAP302', 'TAP201'),
        ('TAP303', 'TAP201'),
        ('TAP304', 'TAP203'),
        ('TAP305', 'TAP205'),
        ('TAP306', 'TAP201'),
        ('TAP306', 'TAP202'),
        ('TAP401', 'TAP301'),
        ('TAP401', 'TAP302'),
        ('TAP402', 'TAP301'),
        ('TAP403', 'TAP301'),
        ('TAP403', 'TAP305'),
        ('TAP404', 'TAP302'),
        ('TAP405', 'TAP301'),
        ('TAP405', 'TAP302'),
        ('TAP405', 'TAP306'),
        ('TAP406', 'TAP302'),
        ('TAD201', 'TAD101'),
        ('TAD202', 'TAD102'),
        ('TAD203', 'TAD104'),
        ('TAD204', 'TAD103'),
        ('TAD204', 'TAD101'),
        ('TAD205', 'TAD103'),
        ('TAD301', 'TAD202'),
        ('TAD302', 'TAD201'),
        ('TAD302', 'TAD202'),
        ('TAD303', 'TAD203'),
        ('TAD303', 'TAD205'),
        ('TAD304', 'TAD201'),
        ('TAD304', 'TAD203'),
        ('TAD305', 'TAD205'),
        ('TAD306', 'TAD103'),
        ('TAD401', 'TAD301'),
        ('TAD401', 'TAD302'),
        ('TAD402', 'TAD304'),
        ('TAD403', 'TAD301'),
        ('TAD403', 'TAD303'),
        ('TAD404', 'TAD302'),
        ('TAD404', 'TAD304'),
        ('TAD405', 'TAD302'),
        ('TAD405', 'TAD303'),
        ('TAD405', 'TAD305'),
        ('TCS201', 'TCS101'),
        ('TCS202', 'TCS103'),
        ('TCS203', 'TCS104'),
        ('TCS204', 'TCS102'),
        ('TCS204', 'TCS103'),
        ('TCS205', 'TCS101'),
        ('TCS301', 'TCS202'),
        ('TCS301', 'TCS204'),
        ('TCS302', 'TCS203'),
        ('TCS302', 'TCS204'),
        ('TCS303', 'TCS204'),
        ('TCS304', 'TCS201'),
        ('TCS304', 'TCS204'),
        ('TCS305', 'TCS202'),
        ('TCS305', 'TCS204'),
        ('TCS306', 'TCS204'),
        ('TCS401', 'TCS301'),
        ('TCS401', 'TCS302'),
        ('TCS402', 'TCS305'),
        ('TCS403', 'TCS301'),
        ('TCS403', 'TCS302'),
        ('TCS404', 'TCS302'),
        ('TCS405', 'TCS301'),
        ('TCS405', 'TCS302'),
        ('TCS405', 'TCS305'),
        ('TCS406', 'TCS204')
)
INSERT INTO subject_prerequisites (subject_id, prerequisite_id)
SELECT s.id, pre.id
FROM prerequisites r
JOIN subjects s ON s.code = r.subject_code
JOIN subjects pre ON pre.code = r.prerequisite_code AND pre.study_plan_id = s.study_plan_id
WHERE NOT EXISTS (
    SELECT 1 FROM subject_prerequisites existing
    WHERE existing.subject_id = s.id AND existing.prerequisite_id = pre.id
);

-- Comprobar que las carreras oficiales realmente tienen sus 24 asignaturas.
DO $$
DECLARE
    n INTEGER;
BEGIN
    SELECT COUNT(*) INTO n FROM subjects s JOIN study_plans p ON p.id = s.study_plan_id JOIN careers c ON c.id = p.career_id WHERE c.code IN ('10450', '10451', '10452');
    IF n <> 72 THEN
        RAISE EXCEPTION 'Se esperaban 72 asignaturas oficiales, pero se encontraron %', n;
    END IF;
END $$;

COMMIT;

-- Resumen posterior:
SELECT c.code AS career_code, c.name, p.identifier AS study_plan, p.status, COUNT(s.id) AS subjects
FROM careers c JOIN study_plans p ON p.career_id = c.id
LEFT JOIN subjects s ON s.study_plan_id = p.id
WHERE c.code IN ('10450', '10451', '10452')
GROUP BY c.code, c.name, p.identifier, p.status
ORDER BY c.code, p.identifier;
