import { Alert, Button, MenuItem, Stack, TextField, Typography } from "@mui/material";
import { Link as RouterLink, useLocation, useSearchParams } from "react-router-dom";
import TeacherTable from "../../components/teacher/TeacherTable";
import { teacherMatchesSearch, useTeacherCatalog } from "../../services/teacherAcademic";

function TeacherSections() {
    // get the assigned sections loaded by TeacherAcademicProvider
    const catalog = useTeacherCatalog();
    const location = useLocation();
    const [params, setParams] = useSearchParams();
    const search = params.get("search") || "";
    // Defaults to the first open period while preserving an explicit historical filter.
    const periodId = params.has("period") ? params.get("period") || "" : String(catalog.currentPeriod?.id || "none");
    const status = params.get("status") || "";
    const changeFilter = (field, value) => {
        // Preserves the other filters when updating the URL
        const next = new URLSearchParams(params);
        if (value || field === "period") next.set(field, value);
        else next.delete(field);
        setParams(next, { replace: true });
    };
    const periods = catalog.periods.map(period => [period.id, period.code]);
    const statuses = ["ACTIVA", "INACTIVA"];
    const rows = catalog.sections.filter(section => (!periodId || String(section.academicPeriodId) === periodId)
        && (!status || section.status === status) && teacherMatchesSearch(section, search, ["subjectCode", "subjectName"]));
    const returnTo = location.pathname + location.search;

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Mis secciones</Typography>
        {!catalog.loading && !catalog.error && !catalog.currentPeriod && !params.has("period") && <Alert severity="info">No hay períodos académicos abiertos. Puedes seleccionar otro período para consultar tus secciones.</Alert>}
        <Stack direction="row" spacing={2} alignItems="center">
            <TextField size="small" label="Buscar por código o asignatura" value={search} onChange={event => changeFilter("search", event.target.value)} sx={{ width: 340 }} />
            <TextField size="small" select label="Período académico" value={periodId} onChange={event => changeFilter("period", event.target.value)} sx={{ width: 220 }}>
                {!catalog.currentPeriod && <MenuItem value="none" disabled>Sin períodos abiertos</MenuItem>}
                <MenuItem value="">Todos</MenuItem>{periods.map(([id, code]) => <MenuItem key={id} value={String(id)}>{code}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Estado" value={status} onChange={event => changeFilter("status", event.target.value)} sx={{ width: 180 }}>
                <MenuItem value="">Todos</MenuItem>{statuses.map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <Button size="small" onClick={() => setParams({}, { replace: true })}>Limpiar filtros</Button>
        </Stack>
        <TeacherTable title="Mis secciones" records={{ ...catalog, rows }} emptyMessage={catalog.sections.length ? "No se encontraron secciones con los filtros seleccionados." : "No tienes secciones asignadas."} columns={[
            ["subjectCode", "Código"], ["subjectName", "Asignatura"], ["periodCode", "Período"], ["code", "Sección"], ["blocks", "Bloques"],
            ["capacity", "Capacidad"], ["enrolledStudents", "Cupos ocupados"], ["status", "Estado de sección"],
            ["students", "Estudiantes", row => <Button component={RouterLink} to={`/teacher/sections/${row.id}/students`} state={{ returnTo }}>Ver</Button>],
            ["grades", "Calificaciones", row => <Button component={RouterLink} to={`/teacher/sections/${row.id}/grades`} state={{ returnTo }}>Ver</Button>]
        ]} />
    </Stack>;
}

export default TeacherSections;
