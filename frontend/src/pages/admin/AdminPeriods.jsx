import { useState } from "react";
import { Alert, Button, MenuItem, Stack, TextField, Typography } from "@mui/material";
import http from "../../http-common";
import AdminTable from "../../components/admin/AdminTable";
import { AdminForm, AdminQuery } from "../../components/admin/AdminDialog";
import { filterValues, matchesFilter, matchesSearch, useAdminRecords } from "../../services/adminAcademic";

function AdminPeriods() {
    const periods = useAdminRecords("/academic-periods");
    const [search, setSearch] = useState("");
    const [year, setYear] = useState("");
    const [semester, setSemester] = useState("");
    const [status, setStatus] = useState("");
    const [form, setForm] = useState(null);
    const [closing, setClosing] = useState(null);
    const [query, setQuery] = useState(null);
    const [message, setMessage] = useState("");

    const rows = periods.rows.filter(row => matchesSearch(row, search, ["code", "year", "status"])
        && matchesFilter(row.year, year) && matchesFilter(row.semester, semester) && matchesFilter(row.status, status));

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Períodos académicos</Typography>
        {message && <Alert severity="success" onClose={() => setMessage("")}>{message}</Alert>}
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: "wrap", alignItems: "center" }}>
            <TextField size="small" label="Buscar por código, año o estado" value={search} onChange={event => setSearch(event.target.value)} sx={{ width: 320 }} />
            <TextField size="small" select label="Año" value={year} onChange={event => setYear(event.target.value)} sx={{ width: 130 }}>
                <MenuItem value="">Todos</MenuItem>
                {filterValues(periods.rows, "year", year).map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Semestre académico" value={semester} onChange={event => setSemester(event.target.value)} sx={{ width: 200 }}>
                <MenuItem value="">Todos</MenuItem>
                {filterValues(periods.rows, "semester", semester).filter(value => Number(value) >= 1 && Number(value) <= 2).map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Estado" value={status} onChange={event => setStatus(event.target.value)} sx={{ width: 180 }}>
                <MenuItem value="">Todos</MenuItem>
                {filterValues(periods.rows, "status", status).map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <Button size="small" onClick={() => { setSearch(""); setYear(""); setSemester(""); setStatus(""); }}>Limpiar filtros</Button>
            <Button variant="contained" onClick={() => setForm({ year: "", semester: 1 })}>Crear período</Button>
        </Stack>
        <AdminTable title="Períodos académicos" filtered records={{ ...periods, rows }} columns={[["code", "Código"], ["year", "Año"], ["semester", "Semestre"], ["status", "Estado"], ["sections", "Secciones", row => <Button onClick={() => setQuery({ title: `Secciones del período ${row.code}`, endpoint: `/sections/academic-period/${row.id}`, columns: [["id", "Sección"], ["subjectId", "Asignatura (ID)"], ["teacherName", "Docente"], ["blocks", "Bloques"], ["capacity", "Capacidad"], ["enrolledStudents", "Inscritos"], ["status", "Estado"]] })}>Ver</Button>]]} actions={row => <>
            {row.status === "ABIERTO" && <Button color="error" onClick={() => setClosing(row)}>Cerrar período</Button>}
        </>} />
        {form && <AdminForm title="Crear período académico" onClose={() => setForm(null)} onSubmit={async () => { await http.post("/academic-periods", { year: Number(form.year), semester: Number(form.semester) }); periods.reload(); setMessage("Período creado correctamente."); }}>
            <TextField required label="Año" type="number" value={form.year} slotProps={{ htmlInput: { min: 2000, step: 1 } }} onChange={event => setForm({ ...form, year: event.target.value })} />
            <TextField required select label="Semestre" value={form.semester} onChange={event => setForm({ ...form, semester: event.target.value })}>
                <MenuItem value={1}>1</MenuItem><MenuItem value={2}>2</MenuItem>
            </TextField>
        </AdminForm>}
        {closing && <AdminForm title={`Cerrar período ${closing.code}`} submitLabel="Confirmar cierre" onClose={() => setClosing(null)} onSubmit={async () => { await http.patch(`/academic-periods/${closing.id}/close`); periods.reload(); setMessage("Período cerrado correctamente."); }}>
            <Alert severity="warning">El backend verificará las calificaciones pendientes y actualizará los estados académicos que correspondan. El cierre impide nuevas inscripciones y cambios en las secciones del período. No existe una operación de reapertura.</Alert>
        </AdminForm>}
        {query && <AdminQuery {...query} onClose={() => setQuery(null)} />}
    </Stack>;
}

export default AdminPeriods;
