-- Appendix A from enunciado.pdf. Load only into an empty academic database.
-- This script creates no people, accounts, periods, sections, enrollments or grades.
BEGIN;

INSERT INTO public.careers (id, code, name, description, duration, status) VALUES
(1, '10450', 'Técnico Analista Programador', 'Carrera técnica de cuatro semestres: Técnico Analista Programador', 4, 'ACTIVA');
INSERT INTO public.study_plans (id, identifier, status, career_id) VALUES
(1, '2021.3', 'VIGENTE', 1);

INSERT INTO public.careers (id, code, name, description, duration, status) VALUES
(2, '10451', 'Técnico en Análisis de Datos', 'Carrera técnica de cuatro semestres: Técnico en Análisis de Datos', 4, 'ACTIVA');
INSERT INTO public.study_plans (id, identifier, status, career_id) VALUES
(2, '2022.3', 'VIGENTE', 2);

INSERT INTO public.careers (id, code, name, description, duration, status) VALUES
(3, '10452', 'Técnico en Ciberseguridad', 'Carrera técnica de cuatro semestres: Técnico en Ciberseguridad', 4, 'ACTIVA');
INSERT INTO public.study_plans (id, identifier, status, career_id) VALUES
(3, '2021.3', 'VIGENTE', 3);

INSERT INTO public.subjects (id, code, name, semester, theory_hours, exercise_hours, laboratory_hours, sct, study_plan_id) VALUES
(1, 'TAP101', 'Fundamentos de Programación', 1, 2, 2, 2, 6, 1),
(2, 'TAP102', 'Matemática Aplicada', 1, 2, 2, 0, 5, 1),
(3, 'TAP103', 'Fundamentos de Computación', 1, 2, 0, 2, 4, 1),
(4, 'TAP104', 'Bases de Datos I', 1, 2, 0, 2, 5, 1),
(5, 'TAP105', 'Comunicación Efectiva', 1, 2, 2, 0, 4, 1),
(6, 'TAP106', 'Inglés Técnico I', 1, 2, 2, 0, 4, 1),
(7, 'TAP201', 'Programación Orientada a Objetos', 2, 2, 2, 2, 6, 1),
(8, 'TAP202', 'Estructuras de Datos', 2, 2, 2, 2, 6, 1),
(9, 'TAP203', 'Bases de Datos II', 2, 2, 0, 2, 5, 1),
(10, 'TAP204', 'Desarrollo Web I', 2, 2, 0, 4, 6, 1),
(11, 'TAP205', 'Sistemas Operativos', 2, 2, 0, 2, 4, 1),
(12, 'TAP206', 'Inglés Técnico II', 2, 2, 2, 0, 3, 1),
(13, 'TAP301', 'Desarrollo Web II', 3, 2, 0, 4, 6, 1),
(14, 'TAP302', 'Ingeniería de Software', 3, 2, 2, 0, 5, 1),
(15, 'TAP303', 'Desarrollo de Aplicaciones Móviles', 3, 2, 0, 4, 6, 1),
(16, 'TAP304', 'Administración de Bases de Datos', 3, 2, 0, 2, 5, 1),
(17, 'TAP305', 'Redes y Comunicaciones', 3, 2, 0, 2, 4, 1),
(18, 'TAP306', 'Taller de Programación', 3, 0, 0, 4, 4, 1),
(19, 'TAP401', 'Arquitectura y Desarrollo de Software', 4, 2, 0, 4, 6, 1),
(20, 'TAP402', 'Desarrollo de Aplicaciones Cloud', 4, 2, 0, 4, 6, 1),
(21, 'TAP403', 'Seguridad de Aplicaciones', 4, 2, 0, 2, 4, 1),
(22, 'TAP404', 'Calidad y Pruebas de Software', 4, 2, 0, 2, 5, 1),
(23, 'TAP405', 'Proyecto de Desarrollo de Software', 4, 0, 0, 6, 6, 1),
(24, 'TAP406', 'Emprendimiento y Gestión de Proyectos', 4, 2, 2, 0, 3, 1),
(25, 'TAD101', 'Fundamentos de Programación', 1, 2, 2, 2, 6, 2),
(26, 'TAD102', 'Matemática Aplicada', 1, 2, 2, 0, 5, 2),
(27, 'TAD103', 'Introducción al Análisis de Datos', 1, 2, 0, 2, 5, 2),
(28, 'TAD104', 'Bases de Datos I', 1, 2, 0, 2, 5, 2),
(29, 'TAD105', 'Herramientas Computacionales', 1, 0, 0, 4, 4, 2),
(30, 'TAD106', 'Comunicación Efectiva', 1, 2, 2, 0, 4, 2),
(31, 'TAD201', 'Programación para Análisis de Datos', 2, 2, 0, 4, 6, 2),
(32, 'TAD202', 'Probabilidad y Estadística', 2, 2, 2, 2, 6, 2),
(33, 'TAD203', 'Bases de Datos II', 2, 2, 0, 2, 5, 2),
(34, 'TAD204', 'Preparación y Calidad de Datos', 2, 2, 0, 4, 5, 2),
(35, 'TAD205', 'Visualización de Datos', 2, 2, 0, 2, 5, 2),
(36, 'TAD206', 'Inglés Técnico', 2, 2, 2, 0, 3, 2),
(37, 'TAD301', 'Estadística Aplicada', 3, 2, 2, 2, 6, 2),
(38, 'TAD302', 'Minería de Datos', 3, 2, 0, 4, 6, 2),
(39, 'TAD303', 'Inteligencia de Negocios', 3, 2, 0, 4, 5, 2),
(40, 'TAD304', 'Integración y Procesamiento de Datos', 3, 2, 0, 4, 5, 2),
(41, 'TAD305', 'Visualización Avanzada de Datos', 3, 2, 0, 2, 4, 2),
(42, 'TAD306', 'Ética y Protección de Datos', 3, 2, 2, 0, 4, 2),
(43, 'TAD401', 'Fundamentos de Machine Learning', 4, 2, 0, 4, 6, 2),
(44, 'TAD402', 'Big Data y Procesamiento de Datos', 4, 2, 0, 4, 5, 2),
(45, 'TAD403', 'Analítica de Negocios', 4, 2, 0, 2, 5, 2),
(46, 'TAD404', 'Desarrollo de Soluciones de Datos', 4, 2, 0, 4, 5, 2),
(47, 'TAD405', 'Proyecto de Análisis de Datos', 4, 0, 0, 6, 6, 2),
(48, 'TAD406', 'Emprendimiento y Gestión de Proyectos', 4, 2, 2, 0, 3, 2),
(49, 'TCS101', 'Fundamentos de Programación', 1, 2, 2, 2, 6, 3),
(50, 'TCS102', 'Fundamentos de Sistemas Computacionales', 1, 2, 0, 2, 5, 3),
(51, 'TCS103', 'Redes de Computadores I', 1, 2, 0, 2, 5, 3),
(52, 'TCS104', 'Sistemas Operativos', 1, 2, 0, 2, 5, 3),
(53, 'TCS105', 'Matemática Aplicada', 1, 2, 2, 0, 5, 3),
(54, 'TCS106', 'Comunicación Efectiva', 1, 2, 2, 0, 4, 3),
(55, 'TCS201', 'Programación para Ciberseguridad', 2, 2, 0, 4, 5, 3),
(56, 'TCS202', 'Redes de Computadores II', 2, 2, 0, 4, 6, 3),
(57, 'TCS203', 'Administración de Sistemas Linux', 2, 2, 0, 4, 5, 3),
(58, 'TCS204', 'Fundamentos de Ciberseguridad', 2, 2, 2, 2, 6, 3),
(59, 'TCS205', 'Bases de Datos', 2, 2, 0, 2, 5, 3),
(60, 'TCS206', 'Inglés Técnico', 2, 2, 2, 0, 3, 3),
(61, 'TCS301', 'Seguridad de Redes', 3, 2, 0, 4, 6, 3),
(62, 'TCS302', 'Seguridad de Sistemas Operativos', 3, 2, 0, 4, 5, 3),
(63, 'TCS303', 'Criptografía Aplicada', 3, 2, 2, 2, 5, 3),
(64, 'TCS304', 'Seguridad de Aplicaciones Web', 3, 2, 0, 4, 5, 3),
(65, 'TCS305', 'Monitoreo y Detección de Amenazas', 3, 2, 0, 4, 5, 3),
(66, 'TCS306', 'Legislación y Ética en Ciberseguridad', 3, 2, 2, 0, 4, 3),
(67, 'TCS401', 'Ethical Hacking y Pruebas de Penetración', 4, 2, 0, 4, 6, 3),
(68, 'TCS402', 'Gestión de Incidentes de Ciberseguridad', 4, 2, 0, 4, 5, 3),
(69, 'TCS403', 'Seguridad en Cloud', 4, 2, 0, 4, 5, 3),
(70, 'TCS404', 'Análisis Forense Digital', 4, 2, 0, 4, 5, 3),
(71, 'TCS405', 'Proyecto de Ciberseguridad', 4, 0, 0, 6, 6, 3),
(72, 'TCS406', 'Gestión de Riesgos de Ciberseguridad', 4, 2, 2, 0, 3, 3);

INSERT INTO public.subject_prerequisites (subject_id, prerequisite_id) VALUES
(7, 1),
(8, 1),
(9, 4),
(10, 1),
(11, 3),
(12, 6),
(13, 7),
(13, 10),
(14, 7),
(15, 7),
(16, 9),
(17, 11),
(18, 7),
(18, 8),
(19, 13),
(19, 14),
(20, 13),
(21, 13),
(21, 17),
(22, 14),
(23, 13),
(23, 14),
(23, 18),
(24, 14),
(31, 25),
(32, 26),
(33, 28),
(34, 27),
(34, 25),
(35, 27),
(37, 32),
(38, 31),
(38, 32),
(39, 33),
(39, 35),
(40, 31),
(40, 33),
(41, 35),
(42, 27),
(43, 37),
(43, 38),
(44, 40),
(45, 37),
(45, 39),
(46, 38),
(46, 40),
(47, 38),
(47, 39),
(47, 41),
(55, 49),
(56, 51),
(57, 52),
(58, 50),
(58, 51),
(59, 49),
(61, 56),
(61, 58),
(62, 57),
(62, 58),
(63, 58),
(64, 55),
(64, 58),
(65, 56),
(65, 58),
(66, 58),
(67, 61),
(67, 62),
(68, 65),
(69, 61),
(69, 62),
(70, 62),
(71, 61),
(71, 62),
(71, 65),
(72, 58);

SELECT setval(pg_get_serial_sequence('public.careers', 'id'), (SELECT MAX(id) FROM public.careers), true);
SELECT setval(pg_get_serial_sequence('public.study_plans', 'id'), (SELECT MAX(id) FROM public.study_plans), true);
SELECT setval(pg_get_serial_sequence('public.subjects', 'id'), (SELECT MAX(id) FROM public.subjects), true);

DO $$
BEGIN
    IF (SELECT COUNT(*) FROM public.careers) <> 3 OR (SELECT COUNT(*) FROM public.study_plans) <> 3 OR (SELECT COUNT(*) FROM public.subjects) <> 72 OR (SELECT COUNT(*) FROM public.subject_prerequisites) <> 75 THEN
        RAISE EXCEPTION 'The curriculum does not match Appendix A';
    END IF;
END;
$$;

COMMIT;
