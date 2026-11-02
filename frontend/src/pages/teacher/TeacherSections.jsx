import { Button, MenuItem, Stack, TextField, Typography } from "@mui/material";
import { Link as RouterLink, useLocation, useSearchParams } from "react-router-dom";
import TeacherTable from "../../components/teacher/TeacherTable";
import { teacherMatchesSearch, useTeacherCatalog } from "../../services/teacherAcademic";

function TeacherSections() {
    const catalog = useTeacherCatalog();
    const location = useLocation();
    const [params, setParams] = useSearchParams();
    const search = params.get("search") || "";
    const periodId = params.get("period") || "";
    const status = params.get("status") || "";
    const changeFilter = (field, value) => {
        const next = new URLSearchParams(params);
        if (value) next.set(field, value);
        else next.delete(field);
        setParams(next, { replace: true });
    };
    const periods = [...new Map(catalog.sections.map(section => [section.academicPeriodId, section.periodCode])).entries()];
    const statuses = [...new Set(catalog.sections.map(section => section.status))];
    const rows = catalog.sections.filter(section => (!periodId || String(section.academicPeriodId) === periodId)
        && (!status || section.status === status) && teacherMatchesSearch(section, search, ["subjectCode", "subjectName"]));
    const returnTo = location.pathname + location.search;

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Mis secciones</Typography>
        <Stack direction="row" spacing={2} alignItems="center">
            <TextField size="small" label="Buscar por código o asignatura" value={search} onChange={event => changeFilter("search", event.target.value)} sx={{ width: 340 }} />
            <TextField size="small" select label="Período académico" value={periodId} onChange={event => changeFilter("period", event.target.value)} sx={{ width: 220 }}>
                <MenuItem value="">Todos</MenuItem>{periods.map(([id, code]) => <MenuItem key={id} value={String(id)}>{code}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Estado" value={status} onChange={event => changeFilter("status", event.target.value)} sx={{ width: 180 }}>
                <MenuItem value="">Todos</MenuItem>{statuses.map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <Button size="small" onClick={() => setParams({}, { replace: true })}>Limpiar filtros</Button>
        </Stack>
        <TeacherTable title="Mis secciones" records={{ ...catalog, rows }} emptyMessage={catalog.sections.length ? "No se encontraron secciones con los filtros seleccionados." : "No tienes secciones asignadas."} columns={[
            ["subjectCode", "Código"], ["subjectName", "Asignatura"], ["periodCode", "Período"], ["id", "Sección"], ["blocks", "Bloques"],
            ["capacity", "Capacidad"], ["enrolledStudents", "Inscritos"], ["status", "Estado de sección"],
            ["periodStatus", "Estado del período", row => row.periodStatus === "CERRADO" ? "Período cerrado" : row.periodStatus === "ABIERTO" ? "Período abierto" : "No disponible"],
            ["students", "Estudiantes", row => <Button component={RouterLink} to={`/teacher/sections/${row.id}/students`} state={{ returnTo }}>Ver</Button>],
            ["grades", "Calificaciones", row => <Button component={RouterLink} to={`/teacher/sections/${row.id}/grades`} state={{ returnTo }}>Ver</Button>]
        ]} />
    </Stack>;
}

export default TeacherSections;
