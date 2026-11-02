import { useState } from "react";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography } from "@mui/material";import http from "../../http-common";
import { normalizePersonField, preparePersonFields } from "../../services/adminPersonValidation";
import AdminTable from "../../components/admin/AdminTable";
import { AdminForm, AdminQuery } from "../../components/admin/AdminDialog";
import { matchesFilter, matchesSearch, saveAdminFields, useAdminRecords } from "../../services/adminAcademic";

const fields = [["rut", "RUT", 12], ["firstName", "Nombre", 100], ["lastName", "Apellido", 100], ["email", "Correo", 300], ["professionalTitle", "Título profesional", 150], ["academicDegree", "Grado académico", 150]];

function AdminTeachers() {
    const teachers = useAdminRecords("/teachers");
    const [search, setSearch] = useState("");
    const [status, setStatus] = useState("");
    const [form, setForm] = useState(null);
    const [editField, setEditField] = useState("");
    const [fieldErrors, setFieldErrors] = useState({});
    const [remove, setRemove] = useState(null);
    const [query, setQuery] = useState(null);
    const [message, setMessage] = useState("");
    const saved = () => { teachers.reload(); setMessage("Operación realizada correctamente."); };
    const rows = teachers.rows.filter(row => matchesSearch(row, search, ["rut", "firstName", "lastName", "email"]) && matchesFilter(row.status, status));
    const [credentials, setCredentials] = useState(null);
    const [showPassword, setShowPassword] = useState(false);

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
        <Typography variant="h5" fontWeight={600}>Docentes</Typography>
        {message && <Alert severity="success" onClose={() => setMessage("")}>{message}</Alert>}
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: "wrap", alignItems: "center" }}>
            <TextField size="small" label="Buscar por RUT, nombre, apellido o correo" value={search} onChange={event => setSearch(event.target.value)} sx={{ width: 320 }} />
            <TextField size="small" select label="Estado" value={status} onChange={event => setStatus(event.target.value)} sx={{ width: 180 }}>
                <MenuItem value="">Todos</MenuItem>
                {["ACTIVO", "INACTIVO"].map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <Button size="small" onClick={() => { setSearch(""); setStatus(""); }}>Limpiar filtros</Button>
            <Button variant="contained" onClick={() => { setFieldErrors({}); setForm(Object.fromEntries(fields.map(([field]) => [field, ""]))); }}>Registrar docente</Button>
        </Stack>
        <AdminTable title="Docentes" filtered records={{ ...teachers, rows }} columns={[
            ["rut", "RUT"], ["firstName", "Nombre"], ["lastName", "Apellido"], ["email", "Correo"], ["professionalTitle", "Título profesional"], ["academicDegree", "Grado académico"], ["status", "Estado"],
            ["sections", "Secciones", row => <Button onClick={() => setQuery({ title: `Secciones de ${row.firstName} ${row.lastName}`, endpoint: `/sections/teacher/${row.id}`, columns: [["id", "Sección"], ["subjectId", "Asignatura (ID)"], ["academicPeriodId", "Período (ID)"], ["blocks", "Bloques"], ["capacity", "Capacidad"], ["enrolledStudents", "Inscritos"], ["status", "Estado"]] })}>Ver</Button>]
        ]} actions={row => <>
            <Button onClick={() => { setFieldErrors({}); setEditField(""); setForm({ ...row }); }}>Editar</Button>
        </>} />
        {form && <AdminForm title={`${form.id ? "Editar" : "Registrar"} docente`} onClose={() => setForm(null)} onSubmit={async () => {
            if (form.id && editField === "status") {
                await http.put(`/teachers/${form.id}/status`, null, { params: { status: form.status } });
            } else {
                const data = Object.fromEntries(fields.map(([field]) => [field, form[field]]));
                Object.assign(data, preparePersonFields(form, !form.id ? fields.map(([field]) => field) : editField === "name" ? ["firstName", "lastName"] : fields.some(([field]) => field === editField) ? [editField] : [], setFieldErrors));
                if (form.id) await saveAdminFields("/teachers", form.id, data, fields.map(([field]) => field), editField === "name" ? ["firstName", "lastName"] : [editField]);
                else {
                    const response = await http.post("/teachers", data);
                    setCredentials({
                        username: response.data.username,
                        temporaryPassword: response.data.temporaryPassword
                    });
                    setShowPassword(false);
                }
            }
            saved();
        }} editOptions={form.id ? [["rut", "RUN"], ["name", "Nombre"], ["email", "Correo electrónico"], ["professionalTitle", "Título profesional"], ["academicDegree", "Grado académico"], ["status", "Estado"]].map(([value, label]) => ({ value, label, disabled: value === "rut" && /^SEED-2026-/.test(form.rut) })) : undefined}
            selectedOption={editField} onSelectOption={value => { setFieldErrors({}); setEditField(value); }} recordEndpoint={form.id ? `/teachers/${form.id}` : undefined} onLoadRecord={setForm} onRemove={() => setRemove(form)}>
            {fields.filter(([field]) => !form.id || field === editField || (editField === "name" && ["firstName", "lastName"].includes(field))).map(([field, label, maxLength]) => <TextField key={field} required label={label} type="text" value={form[field]} error={Boolean(fieldErrors[field])} helperText={fieldErrors[field]} onBlur={field === "email" ? undefined : () => formatField(field)} slotProps={{ htmlInput: { maxLength, ...(field === "email" ? { inputMode: "email" } : {}) } }} onChange={event => { setForm({ ...form, [field]: event.target.value }); setFieldErrors(errors => ({ ...errors, [field]: "" })); }} />)}
            {form.id && editField === "status" && <>
                <TextField required select label="Estado" value={form.status} onChange={event => setForm({ ...form, status: event.target.value })}>
                    {["ACTIVO", "INACTIVO"].map(status => <MenuItem key={status} value={status}>{status}</MenuItem>)}
                </TextField>
                <Alert severity="warning">No se puede desactivar un docente con secciones en períodos abiertos.</Alert>
            </>}
        </AdminForm>}
        {remove && <AdminForm title="Eliminar docente" submitLabel="Confirmar eliminación" onClose={() => setRemove(null)} onSubmit={async () => { await http.delete(`/teachers/${remove.id}`); saved(); setForm(null); }}>
            <Alert severity="warning">¿Estás segura de que deseas eliminar a {remove.firstName} {remove.lastName} ({remove.rut})? Esta acción es irreversible.</Alert>
        </AdminForm>}
        {query && <AdminQuery {...query} onClose={() => setQuery(null)} />}

        {credentials && <Dialog open onClose={() => {
            setCredentials(null);
            setShowPassword(false);
        }} fullWidth maxWidth="sm">
            <DialogTitle>Docente registrado correctamente</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ pt: 1 }}>
                    <Alert severity="success">
                        El docente fue registrado y su cuenta de Keycloak fue creada correctamente.
                    </Alert>
                    <TextField
                        label="Usuario"
                        value={credentials.username}
                        slotProps={{ input: { readOnly: true } }}
                    />
                    <TextField
                        label="Contraseña temporal"
                        type={showPassword ? "text" : "password"}
                        value={credentials.temporaryPassword}
                        slotProps={{ input: { readOnly: true } }}
                    />
                    <Button onClick={() => setShowPassword(!showPassword)}>
                        {showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                    </Button>
                    <Button onClick={() => navigator.clipboard.writeText(
                        `Usuario: ${credentials.username}\nContraseña temporal: ${credentials.temporaryPassword}`
                    )}>
                        Copiar credenciales
                    </Button>
                    <Alert severity="warning">
                        Guarda estas credenciales antes de cerrar. El docente deberá cambiar su contraseña en el primer inicio de sesión.
                    </Alert>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => {
                    setCredentials(null);
                    setShowPassword(false);
                }}>
                    Cerrar
                </Button>
            </DialogActions>
        </Dialog>}

    </Stack>;
}

export default AdminTeachers;
