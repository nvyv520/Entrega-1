import { useState } from "react";
import { Alert, Button, MenuItem, Stack, Tab, Tabs, TextField, Typography } from "@mui/material";
import http from "../../http-common";
import AdminTable from "../../components/admin/AdminTable";
import { AdminForm, AdminQuery } from "../../components/admin/AdminDialog";
import { matchesFilter, matchesSearch, saveAdminFields, useAdminRecords } from "../../services/adminAcademic";

function AdminCareers() {
    // GET /api/careers
    const careers = useAdminRecords("/careers");
    // GET /api/study-plans
    const plans = useAdminRecords("/study-plans");
    const [tab, setTab] = useState(0);
    const [search, setSearch] = useState("");
    const [careerStatus, setCareerStatus] = useState("");
    const [planStatus, setPlanStatus] = useState("");
    const [careerId, setCareerId] = useState("");
    const [form, setForm] = useState(null);
    const [editField, setEditField] = useState("");
    const [remove, setRemove] = useState(null);
    const [query, setQuery] = useState(null);
    const [message, setMessage] = useState("");
    const records = tab === 0 ? careers : plans;
    // select /api/careers or /api/study-plans according to the active tab
    const endpoint = tab === 0 ? "/careers" : "/study-plans";
    const title = tab === 0 ? "Carreras" : "Planes de estudio";
    
    const prerequisiteNames = (row, subjects) => row.prerequisiteIds.map(id => {
        const subject = subjects.find(subject => subject.id === id);
        return subject ? `${subject.code} — ${subject.name}` : "No disponible";
    }).join(", ") || "Sin prerrequisitos";

    const rows = records.rows.filter(row => matchesSearch(row, search, tab === 0 ? ["code", "name"] : ["identifier"])
        && (tab === 0 || matchesFilter(row.careerId, careerId)) && matchesFilter(row.status, tab === 0 ? careerStatus : planStatus));

    const saved = () => {
        careers.reload();
        plans.reload();
        setMessage("Operación realizada correctamente.");
    };

    // create, update or change status using the selected endpoint
    const save = async () => {
        // Uses the status endpoint separately from the general record update
        if (form.id && editField === "status") {
            await http.patch(`${endpoint}/${form.id}/status`, null, { params: { status: form.status } });
            saved();
            return;
        }
        const data = tab === 0
            ? { code: form.code.trim(), name: form.name.trim(), description: form.description.trim() }
            : { identifier: form.identifier.trim(), careerId: Number(form.careerId) };
        if (form.id) await saveAdminFields(endpoint, form.id, data, tab === 0 ? ["code", "name", "description"] : ["identifier", "careerId"], [editField]);
        else await http.post(endpoint, data);
        saved();
    };

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Carreras y planes de estudio</Typography>
        <Tabs value={tab} onChange={(_, value) => { setTab(value); setSearch(""); setMessage(""); }}>
            <Tab label="Carreras" /><Tab label="Planes de estudio" />
        </Tabs>
        {message && <Alert severity="success" onClose={() => setMessage("")}>{message}</Alert>}
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: "wrap", alignItems: "center" }}>
            <TextField size="small" label="Buscar por código, nombre o identificador" value={search} onChange={event => setSearch(event.target.value)} sx={{ width: 320 }} />
            {tab === 1 && <TextField size="small" select label="Carrera" value={careerId} onChange={event => setCareerId(event.target.value)} sx={{ width: 300 }}>
                <MenuItem value="">Todas</MenuItem>
                {careers.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.name}</MenuItem>)}
            </TextField>}
            {tab === 0 ? (
                <TextField size="small" select label="Estado" value={careerStatus} onChange={event => setCareerStatus(event.target.value)} sx={{ width: 180 }}>
                    <MenuItem value="">Todas</MenuItem>
                    {["ACTIVA", "INACTIVA"].map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
                </TextField>
            ) : (
                <TextField size="small" select label="Vigencia" value={planStatus} onChange={event => setPlanStatus(event.target.value)} sx={{ width: 180 }}>
                    <MenuItem value="">Todos</MenuItem>
                    <MenuItem value="VIGENTE">VIGENTE</MenuItem>
                    <MenuItem value="NO_VIGENTE">NO VIGENTE</MenuItem>
                </TextField>
            )}
            <Button size="small" onClick={() => { setSearch(""); if (tab === 0) setCareerStatus(""); else { setCareerId(""); setPlanStatus(""); } }}>Limpiar filtros</Button>
            <Button variant="contained" onClick={() => setForm(tab === 0 ? { code: "", name: "", description: "" } : { identifier: "", careerId })}>Crear {tab === 0 ? "carrera" : "plan"}</Button>
        </Stack>
        {tab === 1 && careers.error && <Alert severity="error" action={<Button onClick={careers.reload}>Reintentar</Button>}>{careers.error}</Alert>}
        <AdminTable title={title} filtered records={{ ...records, rows }} columns={tab === 0 ? [["code", "Código"], ["name", "Carrera"], ["description", "Descripción"], ["duration", "Duración"], ["status", "Estado"], ["plans", "Planes", row => <Button onClick={() => { setCareerId(row.id); setTab(1); setSearch(""); setPlanStatus(""); }}>Ver</Button>]] : [["identifier", "Identificador", row => `${row.identifier} (${row.careerCode})`], ["careerCode", "Carrera"], ["status", "Vigencia"], ["subjects", "Asignaturas", row => <Button onClick={() => setQuery({ title: `Asignaturas del plan ${row.identifier} (${row.careerCode})`, endpoint: `/subjects/study-plan/${row.id}`, sortRows: (first, second) => Number(first.semester) - Number(second.semester) || first.code.localeCompare(second.code, "es-CL"), columns: [["code", "Código"], ["name", "Asignatura"], ["semester", "Semestre"], ["sct", "SCT"], ["prerequisiteIds", "Prerrequisitos", prerequisiteNames]] })}>Ver</Button>]]}
            actions={row => <>
                <Button onClick={() => { setEditField(""); setForm({ ...row }); }}>Editar</Button>
            </>} />
        {form && <AdminForm title={`${form.id ? "Editar" : "Crear"} ${tab === 0 ? "carrera" : "plan"}`} onClose={() => setForm(null)} onSubmit={save}
            editOptions={form.id ? (tab === 0 ? [["code", "Código"], ["name", "Nombre"], ["description", "Descripción"], ["status", "Estado"]] : [["identifier", "Identificador"], ["status", "Vigencia"]]).map(([value, label]) => ({ value, label })) : undefined}
            selectedOption={editField} onSelectOption={setEditField} recordEndpoint={form.id ? `${endpoint}/${form.id}` : undefined} onLoadRecord={setForm} onRemove={() => setRemove(form)}>
            {tab === 0 ? <>
                {(!form.id || editField === "code") && <TextField required label="Código" value={form.code} slotProps={{ htmlInput: { maxLength: 6 } }} onChange={event => setForm({ ...form, code: event.target.value })} />}
                {(!form.id || editField === "name") && <TextField required label="Nombre" value={form.name} slotProps={{ htmlInput: { maxLength: 100 } }} onChange={event => setForm({ ...form, name: event.target.value })} />}
                {(!form.id || editField === "description") && <TextField required multiline label="Descripción" value={form.description} slotProps={{ htmlInput: { maxLength: 1000 } }} onChange={event => setForm({ ...form, description: event.target.value })} />}
            </> : <>
                {(!form.id || editField === "identifier") && <TextField required label="Identificador" value={form.identifier} slotProps={{ htmlInput: { maxLength: 50 } }} onChange={event => setForm({ ...form, identifier: event.target.value })} />}
                {!form.id && <TextField required select label="Carrera" value={form.careerId} onChange={event => setForm({ ...form, careerId: event.target.value })}>
                    {careers.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.name}</MenuItem>)}
                </TextField>}
                {!form.id && <Alert severity="info">Los planes nuevos se crean NO_VIGENTE. La carrera de un plan no se puede cambiar.</Alert>}
            </>}
            {form.id && editField === "status" && <>
                <TextField required select label={tab === 0 ? "Estado" : "Vigencia"} value={form.status} onChange={event => setForm({ ...form, status: event.target.value })}>
                    {(tab === 0 ? ["ACTIVA", "INACTIVA"] : ["VIGENTE", "NO_VIGENTE"]).map(status => <MenuItem key={status} value={status}>{status}</MenuItem>)}
                </TextField>
                {tab === 1 && <Alert severity="warning">Al establecer VIGENTE, el backend dejará NO_VIGENTE el plan anterior de esta carrera. No se reasignarán estudiantes.</Alert>}
            </>}
        </AdminForm>}
        {remove && <AdminForm title={`Eliminar ${tab === 0 ? "carrera" : "plan de estudio"}`} submitLabel="Confirmar eliminación" onClose={() => setRemove(null)} onSubmit={async () => { await http.delete(`${endpoint}/${remove.id}`); saved(); setForm(null); }}>
            <Alert severity="warning">¿Estás segura de que deseas eliminar {tab === 0 ? `la carrera ${remove.code} — ${remove.name}` : `el plan ${remove.identifier} (${remove.careerCode})`}? Esta acción es irreversible.</Alert>
        </AdminForm>}
        {query && <AdminQuery {...query} onClose={() => setQuery(null)} />}
    </Stack>;
}

export default AdminCareers;
