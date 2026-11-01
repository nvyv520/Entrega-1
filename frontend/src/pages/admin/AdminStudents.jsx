import { useState } from "react";
import { Alert, Button, MenuItem, Stack, TextField, Typography } from "@mui/material";
import http from "../../http-common";
import { normalizePersonField, preparePersonFields } from "../../services/adminPersonValidation";
import AdminTable from "../../components/admin/AdminTable";
import { AdminForm, AdminQuery } from "../../components/admin/AdminDialog";
import { filterValues, matchesFilter, matchesSearch, saveAdminFields, useAdminRecords } from "../../services/adminAcademic";

const fields = [["rut", "RUT", 12], ["firstName", "Nombre", 100], ["lastName", "Apellido", 100], ["email", "Correo", 300]];

function AdminStudents() {
    const students = useAdminRecords("/students");
    const careers = useAdminRecords("/careers");
    const plans = useAdminRecords("/study-plans");
    const enrollments = useAdminRecords("/enrollments");
    const matriculations = useAdminRecords("/period-matriculations");
    const [search, setSearch] = useState("");
    const [careerId, setCareerId] = useState("");
    const [studyPlanId, setStudyPlanId] = useState("");
    const [status, setStatus] = useState("");
    const [form, setForm] = useState(null);
    const [editField, setEditField] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [remove, setRemove] = useState(null);
    const [query, setQuery] = useState(null);
    const [message, setMessage] = useState("");
    const saved = () => { students.reload(); setMessage("Operación realizada correctamente."); };
    const selectorsReady = !careers.loading && !plans.loading && !careers.error && !plans.error;
    const hasHistory = id => enrollments.rows.some(row => row.studentId === id) || matriculations.rows.some(row => row.studentId === id);
    const relationsLocked = form?.id && (hasHistory(form.id) || enrollments.loading || matriculations.loading || Boolean(enrollments.error || matriculations.error));
    const careerName = id => careers.rows.find(row => row.id === id)?.name || id;
    const planName = id => plans.rows.find(row => row.id === id)?.identifier || id;

    const rows = students.rows.filter(row => matchesSearch(row, search, ["rut", "firstName", "lastName", "email"])
        && matchesFilter(row.careerId, careerId) && matchesFilter(row.studyPlanId, studyPlanId) && matchesFilter(row.status, status));

    const formatField = field => {
        try {
            const value = normalizePersonField(field, form[field]);
            setForm({ ...form, [field]: value });
            setFieldErrors(errors => ({ ...errors, [field]: "" }));
        } catch (error) {
            setFieldErrors(errors => ({ ...errors, [field]: error.message }));
        }
    };

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Estudiantes</Typography>
        {message && <Alert severity="success" onClose={() => setMessage("")}>{message}</Alert>}
        {[careers, plans, enrollments, matriculations].filter(records => records.error).map((records, index) => <Alert key={index} severity="error" action={<Button onClick={records.reload}>Reintentar</Button>}>{records.error}</Alert>)}
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: "wrap", alignItems: "center" }}>
            <TextField size="small" label="Buscar por RUT, nombre, apellido o correo" value={search} onChange={event => setSearch(event.target.value)} sx={{ width: 320 }} />
            <TextField size="small" select label="Carrera" value={careerId} onChange={event => { const value = event.target.value; setCareerId(value); if (value && !plans.rows.some(row => String(row.id) === String(studyPlanId) && String(row.careerId) === String(value))) setStudyPlanId(""); }} sx={{ width: 180 }} disabled={careers.loading || Boolean(careers.error)}>
                <MenuItem value="">Todas</MenuItem>
                {careers.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.name}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Plan de estudio" value={studyPlanId} onChange={event => setStudyPlanId(event.target.value)} sx={{ width: 180 }} disabled={plans.loading || Boolean(plans.error)}>
                <MenuItem value="">Todos</MenuItem>
                {plans.rows.filter(row => matchesFilter(row.careerId, careerId)).map(row => <MenuItem key={row.id} value={row.id}>{row.identifier} (carrera {row.careerId})</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Estado académico" value={status} onChange={event => setStatus(event.target.value)} sx={{ width: 200 }}>
                <MenuItem value="">Todos</MenuItem>
                {filterValues(students.rows, "status", status).map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <Button size="small" onClick={() => { setSearch(""); setCareerId(""); setStudyPlanId(""); setStatus(""); }}>Limpiar filtros</Button>
            <Button variant="contained" disabled={!selectorsReady} onClick={() => { setFieldErrors({}); setForm({ rut: "", firstName: "", lastName: "", email: "", careerId: "", studyPlanId: "" }); }}>Registrar estudiante</Button>
        </Stack>
        <AdminTable title="Estudiantes" filtered records={{ ...students, rows }} columns={[
            ["rut", "RUT"], ["firstName", "Nombre"], ["lastName", "Apellido"], ["email", "Correo"], ["careerId", "Carrera", row => careerName(row.careerId)], ["studyPlanId", "Plan", row => planName(row.studyPlanId)], ["status", "Estado"],
            ["history", "Historial", row => <Button onClick={() => setQuery({ title: `Historial de ${row.firstName} ${row.lastName}`, endpoint: `/students/${row.id}/academic-history`, columns: [["subjectCode", "Código"], ["subjectName", "Asignatura"], ["subjectSemester", "Semestre"], ["academicPeriodCode", "Período"], ["enrollmentStatus", "Inscripción"], ["finalGrade", "Nota final"], ["result", "Resultado"]] })}>Ver</Button>]
        ]} actions={row => <>
            <Button onClick={() => { setFieldErrors({}); setEditField(""); setForm({ ...row }); }}>Editar</Button>
        </>} />
        {form && <AdminForm title={`${form.id ? "Editar" : "Registrar"} estudiante`} onClose={() => setForm(null)} onSubmit={async () => {
            if (form.id && editField === "status") {
                await http.patch(`/students/${form.id}/status`, null, { params: { status: form.status } });
            } else {
                const data = { ...Object.fromEntries(fields.map(([field]) => [field, form[field]])), careerId: Number(form.careerId), studyPlanId: form.studyPlanId ? Number(form.studyPlanId) : null };
                Object.assign(data, preparePersonFields(form, !form.id ? fields.map(([field]) => field) : editField === "name" ? ["firstName", "lastName"] : fields.some(([field]) => field === editField) ? [editField] : []));
                if (form.id) await saveAdminFields("/students", form.id, data, [...fields.map(([field]) => field), "careerId", "studyPlanId"], editField === "name" ? ["firstName", "lastName"] : editField === "relations" ? ["careerId", "studyPlanId"] : [editField]);
                else { delete data.studyPlanId; await http.post("/students", data); }
            }
            saved();
        }} editOptions={form.id ? [
            { value: "rut", label: "RUN" }, { value: "name", label: "Nombre" }, { value: "email", label: "Correo electrónico" },
            { value: "relations", label: "Carrera y plan", disabled: Boolean(relationsLocked) || !selectorsReady },
            { value: "status", label: "Estado académico", disabled: ["EGRESADO", "ELIMINADO"].includes(form.status) }
        ] : undefined} selectedOption={editField} onSelectOption={value => { setFieldErrors({}); setEditField(value); }} recordEndpoint={form.id ? `/students/${form.id}` : undefined} onLoadRecord={setForm} onRemove={() => setRemove(form)} removeDisabled={enrollments.loading || matriculations.loading || Boolean(enrollments.error || matriculations.error) || hasHistory(form.id)}>
            {fields.filter(([field]) => !form.id || field === editField || (editField === "name" && ["firstName", "lastName"].includes(field))).map(([field, label, maxLength]) => <TextField key={field} required label={label} type="text" value={form[field]} error={Boolean(fieldErrors[field])} helperText={fieldErrors[field]} onBlur={field === "email" ? undefined : () => formatField(field)} slotProps={{ htmlInput: { maxLength, ...(field === "email" ? { inputMode: "email" } : {}) } }} onChange={event => { setForm({ ...form, [field]: event.target.value }); setFieldErrors(errors => ({ ...errors, [field]: "" })); }} />)}
            {(!form.id || editField === "relations") && <TextField required select label="Carrera" disabled={Boolean(relationsLocked)} value={form.careerId} onChange={event => setForm({ ...form, careerId: event.target.value, studyPlanId: "" })}>
                {careers.rows.filter(row => row.status === "ACTIVA" || row.id === form.careerId).map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.name}</MenuItem>)}
            </TextField>}
            {form.id && editField === "relations" && <TextField select label="Plan de estudio" disabled={Boolean(relationsLocked)} value={form.studyPlanId} onChange={event => setForm({ ...form, studyPlanId: event.target.value })}>
                <MenuItem value="">Asignar plan vigente al cambiar de carrera</MenuItem>
                {plans.rows.filter(row => String(row.careerId) === String(form.careerId)).map(row => <MenuItem key={row.id} value={row.id}>{row.identifier} — {row.status}</MenuItem>)}
            </TextField>}
            {(!form.id || editField === "relations") && <Alert severity="info">El backend asigna el plan vigente al registrar. No se permite cambiar carrera o plan si existe información académica.</Alert>}
            {form.id && editField === "status" && <>
                <Alert severity="warning">EGRESADO y ELIMINADO se calculan automáticamente y son irreversibles.</Alert>
                <TextField required select label="Estado académico" value={form.status} onChange={event => setForm({ ...form, status: event.target.value })}>
                    {["REGULAR", "POSTERGACION", "RETIRO_TEMPORAL"].map(status => <MenuItem key={status} value={status}>{status}</MenuItem>)}
                </TextField>
            </>}
        </AdminForm>}
        {remove && <AdminForm title="Eliminar estudiante" submitLabel="Confirmar eliminación" onClose={() => setRemove(null)} onSubmit={async () => { await http.delete(`/students/${remove.id}`); saved(); setForm(null); }}>
            <Alert severity="warning">¿Estás segura de que deseas eliminar a {remove.firstName} {remove.lastName} ({remove.rut})? Esta acción es irreversible.</Alert>
        </AdminForm>}
        {query && <AdminQuery {...query} onClose={() => setQuery(null)} />}
    </Stack>;
}

export default AdminStudents;
