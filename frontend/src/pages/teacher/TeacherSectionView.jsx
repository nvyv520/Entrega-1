import { useEffect, useState } from "react";
import PropTypes from "prop-types";
import { Link as RouterLink, useLocation, useParams, useSearchParams } from "react-router-dom";
import { Alert, Button, CircularProgress, MenuItem, Stack, TextField, Typography } from "@mui/material";
import TeacherTable from "../../components/teacher/TeacherTable";
import TeacherGradeDialog from "../../components/teacher/TeacherGradeDialog";
import { formatFinalGrade, teacherMatchesSearch, useTeacherCatalog, useTeacherRecords } from "../../services/teacherAcademic";

function TeacherSectionView({ grades = false }) {
    const catalog = useTeacherCatalog();
    const { sectionId } = useParams();
    const location = useLocation();
    const [params, setParams] = useSearchParams();
    const selectedId = sectionId || params.get("section") || "";
    // Explicit section links keep their period; other visits use the first open period.
    const periodId = params.has("period") ? params.get("period") || "" : String(
        catalog.sections.find(section => String(section.id) === selectedId)?.academicPeriodId || catalog.currentPeriod?.id || "none"
    );
    const search = params.get("search") || "";
    const section = catalog.sections.find(section => String(section.id) === selectedId
        && (sectionId || !periodId || String(section.academicPeriodId) === periodId));
    // Requests use an assigned section object, never a raw URL ID from another teacher.
    const students = useTeacherRecords(section ? `/sections/me/${section.id}/students` : null);
    const sectionGrades = useTeacherRecords(grades && section ? `/grades/me/sections/${section.id}` : null);
    const [form, setForm] = useState(null);
    const [message, setMessage] = useState("");

    useEffect(() => { setForm(null); setMessage(""); }, [selectedId, grades]);

    const changeFilter = (field, value) => {
        const next = new URLSearchParams(params);
        if (value || field === "period") next.set(field, value);
        else next.delete(field);
        if (field === "period" && value && section && String(section.academicPeriodId) !== value) next.delete("section");
        setParams(next, { replace: true });
    };
    const periods = catalog.periods.map(period => [period.id, period.code]);
    const sources = grades ? [students, sectionGrades] : [students];
    const failed = sources.find(source => source.error);
    const records = {
        rows: students.rows.map(student => ({ ...student, grade: sectionGrades.rows.find(grade => grade.enrollmentId === student.enrollmentId) }))
            .filter(student => teacherMatchesSearch(student, search, ["firstName", "lastName"])),
        loading: sources.some(source => source.loading),
        error: failed?.error || "",
        reload: () => sources.forEach(source => source.reload())
    };
    const canGrade = grades && section?.periodStatus === "ABIERTO" && section?.status === "ACTIVA";
    const previous = location.state?.returnTo;
    const returnTo = typeof previous === "string" && ["/teacher", "/teacher/students", "/teacher/grades"].includes(previous.split("?")[0]) ? previous : "/teacher";
    const title = grades ? "Calificaciones" : "Estudiantes";

    if (catalog.loading) return <CircularProgress aria-label="Cargando secciones" />;
    if (catalog.error) return <Alert severity="error" action={<Button color="inherit" onClick={catalog.reload}>Reintentar</Button>}>{catalog.error}</Alert>;

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>{title}</Typography>
        {!catalog.currentPeriod && !params.has("period") && !selectedId && <Alert severity="info">No hay períodos académicos abiertos. Puedes seleccionar otro período para consultar tus secciones.</Alert>}
        {sectionId && <Button component={RouterLink} to={returnTo} sx={{ alignSelf: "flex-start" }}>Volver</Button>}
        {message && <Alert severity="success" onClose={() => setMessage("")}>{message}</Alert>}
        {!catalog.sections.length ? <Alert severity="info">No tienes secciones asignadas.</Alert> : <>
            <Stack direction="row" spacing={2} alignItems="center">
                {!sectionId && <>
                    <TextField size="small" select label="Período académico" value={periodId} onChange={event => changeFilter("period", event.target.value)} sx={{ width: 220 }}>
                        {!catalog.currentPeriod && <MenuItem value="none" disabled>Sin períodos abiertos</MenuItem>}
                        <MenuItem value="">Todos</MenuItem>{periods.map(([id, code]) => <MenuItem key={id} value={String(id)}>{code}</MenuItem>)}
                    </TextField>
                    <TextField size="small" select label="Sección" value={selectedId} onChange={event => changeFilter("section", event.target.value)} sx={{ width: 460 }}>
                        <MenuItem value="">Seleccionar sección</MenuItem>
                        {catalog.sections.filter(section => !periodId || String(section.academicPeriodId) === periodId).map(section => <MenuItem key={section.id} value={String(section.id)}>{section.subjectCode} — {section.subjectName} · {section.periodCode} · Sección {section.code}</MenuItem>)}
                    </TextField>
                </>}
                <TextField size="small" label="Buscar por nombre o apellido" value={search} onChange={event => changeFilter("search", event.target.value)} sx={{ width: 300 }} />
                <Button size="small" onClick={() => setParams({}, { replace: true })}>Limpiar filtros</Button>
            </Stack>
            {!selectedId ? <Alert severity="info">Selecciona una sección para consultar sus {grades ? "calificaciones" : "estudiantes"}.</Alert> : !section ? <Alert severity="error">La sección no está asignada a tu cuenta docente o no corresponde al período seleccionado.</Alert> : <>
                <Typography variant="h6">{section.subjectCode} — {section.subjectName} · Sección {section.code} · {section.periodCode}</Typography>
                {grades && section.periodStatus === "CERRADO" && <Alert severity="info">El período está cerrado. Las calificaciones solo pueden consultarse.</Alert>}
                {grades && section.periodStatus === "ABIERTO" && section.status !== "ACTIVA" && <Alert severity="info">La sección no está activa. Las calificaciones solo pueden consultarse.</Alert>}
                {grades && !section.periodStatus && <Alert severity="warning">No se pudo determinar el estado del período. El registro y la edición de notas no están disponibles.</Alert>}
                {grades && !records.loading && !records.error && students.rows.some(student => !student.enrollmentId) && <Alert severity="warning">No se recibió la inscripción activa de algunos estudiantes. Sus notas no pueden registrarse hasta disponer de esa información.</Alert>}
                {/* Preserves the return path when opening the section grades */}
                {!grades && <Button component={RouterLink} to={`/teacher/sections/${section.id}/grades`} state={{ returnTo: sectionId ? returnTo : location.pathname + location.search }} sx={{ alignSelf: "flex-start" }}>Ver calificaciones</Button>}
                <TeacherTable title={title} records={records} emptyMessage={students.rows.length ? "No se encontraron estudiantes con la búsqueda seleccionada." : "No hay estudiantes con inscripción activa en esta sección."} columns={[
                    ["firstName", "Nombre"], ["lastName", "Apellido"],
                    ...(grades ? [
                        ["finalGrade", "Nota final", row => formatFinalGrade(row.grade?.finalGrade)],
                        ["result", "Resultado", row => row.grade?.result || "Sin nota"],
                        ...(canGrade ? [["action", "Acción", row => row.enrollmentId ? <Button onClick={() => setForm({ ...row, sectionId: section.id })}>{row.grade ? "Editar" : "Registrar"}</Button> : "Inscripción no disponible"]] : [])
                    ] : [])
                ]} />
                {grades && form?.sectionId === section.id && <TeacherGradeDialog student={form} section={section} onClose={() => setForm(null)} onSaved={grade => {
                    setMessage(`Calificación guardada: ${formatFinalGrade(grade.finalGrade)} — ${grade.result}.`);
                    records.reload();
                }} />}
            </>}
        </>}
    </Stack>;
}

TeacherSectionView.propTypes = { grades: PropTypes.bool };

export default TeacherSectionView;
