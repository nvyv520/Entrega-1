import { useState } from "react";
import { Alert, Button, MenuItem, Stack, TextField, Typography } from "@mui/material";
import http from "../../http-common";
import AdminTable from "../../components/admin/AdminTable";
import { AdminForm } from "../../components/admin/AdminDialog";
import { filterValues, filteredRecords, matchesFilter, matchesSearch, saveAdminFields, useAdminRecords } from "../../services/adminAcademic";

const numberFields = [["semester", "Semestre", 1, 4], ["theoryHours", "Horas de teoría", 0, 7], ["exerciseHours", "Horas de ejercicios", 0, 7], ["laboratoryHours", "Horas de laboratorio", 0, 7], ["sct", "Créditos SCT", 1, 7]];

function AdminSubjects() {
    const subjects = useAdminRecords("/subjects");
    const careers = useAdminRecords("/careers");
    const plans = useAdminRecords("/study-plans");
    const sections = useAdminRecords("/sections");
    const enrollments = useAdminRecords("/enrollments");
    const [search, setSearch] = useState("");
    const [careerId, setCareerId] = useState("");
    const [studyPlanId, setStudyPlanId] = useState("");
    const [semester, setSemester] = useState("");
    const [form, setForm] = useState(null);
    const [editField, setEditField] = useState("");
    const [remove, setRemove] = useState(null);
    const [message, setMessage] = useState("");
    const hasSections = id => sections.rows.some(row => row.subjectId === id);
    const hasHistory = id => enrollments.rows.some(row => sections.rows.some(section => section.id === row.sectionId && section.subjectId === id));
    const restrictionsLoaded = !sections.loading && !sections.error && !enrollments.loading && !enrollments.error;
    const canEdit = row => restrictionsLoaded && !hasHistory(row.id) && !plans.loading && !plans.error;
    const hoursReadOnly = Boolean(form?.id && editField === "hours" && (!canEdit(form) || hasSections(form.id)));
    const editNotice = !form?.id ? "" : !restrictionsLoaded ? "No se pudieron comprobar las inscripciones y secciones. La edición permanece bloqueada hasta completar esas consultas."
        : hasHistory(form.id) ? "Esta asignatura tiene inscripciones registradas y su información académica está protegida: no admite edición. Las horas TEL pueden consultarse."
        : plans.loading || plans.error ? "La edición no está disponible mientras no se pueda consultar el plan de estudio."
        : hasSections(form.id) ? "Las horas TEL no se pueden editar porque existen secciones asociadas. Los demás campos permitidos conservan su edición." : "";
    const prerequisiteName = id => {
        const subject = subjects.rows.find(row => row.id === id);
        return subject ? `${subject.code} — ${subject.name}` : `ID ${id}`;
    };
    const saved = () => { subjects.reload(); setMessage("Operación realizada correctamente."); };

    const rows = subjects.rows.filter(row => matchesSearch(row, search, ["code", "name"]) && matchesFilter(row.studyPlanId, studyPlanId)
        && matchesFilter(row.semester, semester) && matchesFilter(plans.rows.find(plan => plan.id === row.studyPlanId)?.careerId, careerId));

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Asignaturas</Typography>
        {message && <Alert severity="success" onClose={() => setMessage("")}>{message}</Alert>}
        {[careers, plans, sections, enrollments].filter(records => records.error).map((records, index) => <Alert key={index} severity="error" action={<Button onClick={records.reload}>Reintentar</Button>}>{records.error}</Alert>)}
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: "wrap", alignItems: "center" }}>
            <TextField size="small" label="Buscar por código o nombre" value={search} onChange={event => setSearch(event.target.value)} sx={{ width: 320 }} />
            <TextField size="small" select label="Carrera" value={careerId} onChange={event => { const value = event.target.value; setCareerId(value); if (value && !plans.rows.some(row => String(row.id) === String(studyPlanId) && String(row.careerId) === String(value))) setStudyPlanId(""); }} sx={{ width: 180 }} disabled={careers.loading || Boolean(careers.error)}>
                <MenuItem value="">Todas</MenuItem>
                {careers.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.name}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Plan de estudio" value={studyPlanId} onChange={event => setStudyPlanId(event.target.value)} sx={{ width: 180 }} disabled={plans.loading || Boolean(plans.error)}>
                <MenuItem value="">Todos</MenuItem>
                {plans.rows.filter(row => matchesFilter(row.careerId, careerId)).map(row => <MenuItem key={row.id} value={row.id}>{row.identifier} ({careers.rows.find(career => career.id === row.careerId)?.code})</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Semestre curricular" value={semester} onChange={event => setSemester(event.target.value)} sx={{ width: 190 }}>
                <MenuItem value="">Todos</MenuItem>
                {filterValues(subjects.rows, "semester", semester).filter(value => Number(value) >= 1 && Number(value) <= 4).map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <Button size="small" onClick={() => { setSearch(""); setCareerId(""); setStudyPlanId(""); setSemester(""); }}>Limpiar filtros</Button>
            <Button variant="contained" disabled={plans.loading || Boolean(plans.error)} onClick={() => setForm({ code: "", name: "", semester: 1, theoryHours: 0, exerciseHours: 0, laboratoryHours: 0, sct: 1, studyPlanId: "", prerequisiteIds: [] })}>Crear asignatura</Button>
        </Stack>
        <AdminTable title="Asignaturas" filtered records={filteredRecords(subjects, rows, careerId ? [plans] : [])} columns={[
            ["code", "Código"], ["name", "Nombre"], ["studyPlanId", "Plan", row => plans.rows.find(plan => plan.id === row.studyPlanId)?.identifier || row.studyPlanId], ["semester", "Semestre"],
            ["theoryHours", "Teoría"], ["exerciseHours", "Ejercicios"], ["laboratoryHours", "Laboratorio"], ["sct", "SCT"], ["prerequisiteIds", "Prerrequisitos", row => row.prerequisiteIds.map(prerequisiteName).join(", ") || "Sin prerrequisitos"]
        ]} actions={row => <>
            <Button onClick={() => { setEditField(""); setForm({ ...row }); }}>Editar</Button>
        </>} />
        <Alert severity="info">El semestre y el plan no son editables. Las horas se bloquean si hay secciones; las asignaturas con historial académico no admiten edición. El backend valida cada operación.</Alert>
        {form && <AdminForm title={`${form.id ? "Editar" : "Crear"} asignatura`} onClose={() => setForm(null)} onSubmit={async () => {
            if ((!form.id || editField === "hours") && Number(form.theoryHours) + Number(form.exerciseHours) + Number(form.laboratoryHours) > 7) {
                throw new Error("Horas TEL: la suma de teoría, ejercicios y laboratorio no puede superar 7 horas semanales.");
            }
            const data = { code: form.code.trim(), name: form.name.trim(), studyPlanId: Number(form.studyPlanId), prerequisiteIds: form.prerequisiteIds.map(Number) };
            numberFields.forEach(([field]) => { data[field] = Number(form[field]); });
            if (form.id) await saveAdminFields("/subjects", form.id, data, ["code", "name", "studyPlanId", "prerequisiteIds", ...numberFields.map(([field]) => field)], editField === "hours" ? ["theoryHours", "exerciseHours", "laboratoryHours"] : [editField]);
            else await http.post("/subjects", data);
            saved();
        }} editOptions={form.id ? [
            { value: "code", label: "Código" }, { value: "name", label: "Nombre" }, { value: "sct", label: "SCT" },
            { value: "prerequisiteIds", label: "Prerrequisitos" }, { value: "hours", label: canEdit(form) && !hasSections(form.id) ? "Horas TEL" : "Horas TEL (consulta)" }
        ].map(option => ({ ...option, disabled: option.value === "hours" ? !restrictionsLoaded : !canEdit(form) || Boolean(option.disabled) })) : undefined} selectedOption={editField} onSelectOption={setEditField} recordEndpoint={form.id ? `/subjects/${form.id}` : undefined} onLoadRecord={setForm} onRemove={() => setRemove(form)} readOnly={hoursReadOnly} editNotice={editNotice} removeDisabled={!restrictionsLoaded || hasSections(form.id) || subjects.rows.some(subject => subject.prerequisiteIds.includes(form.id))}>
            {(!form.id || editField === "code") && <TextField required label="Código" value={form.code} slotProps={{ htmlInput: { maxLength: 20 } }} onChange={event => setForm({ ...form, code: event.target.value })} />}
            {(!form.id || editField === "name") && <TextField required label="Nombre" value={form.name} slotProps={{ htmlInput: { maxLength: 100 } }} onChange={event => setForm({ ...form, name: event.target.value })} />}
            {!form.id && <TextField required select label="Plan de estudio" value={form.studyPlanId} onChange={event => setForm({ ...form, studyPlanId: event.target.value, prerequisiteIds: [] })}>
                {plans.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.identifier} (carrera {row.careerId})</MenuItem>)}
            </TextField>}
            {numberFields.filter(([field]) => !hoursReadOnly && (!form.id || field === editField || (editField === "hours" && field.endsWith("Hours")))).map(([field, label, min, max]) => <TextField key={field} required type="number" label={label} value={form[field]} slotProps={{ htmlInput: { min, max, step: 1 } }} disabled={Boolean(form.id) && (field === "semester" || (field.endsWith("Hours") && hasSections(form.id)))} onChange={event => setForm({ ...form, [field]: event.target.value, ...(field === "semester" ? { prerequisiteIds: [] } : {}) })} />)}
            {hoursReadOnly && <>
                <Typography>Teoría: {form.theoryHours} · Ejercicios: {form.exerciseHours} · Laboratorio: {form.laboratoryHours}</Typography>
                <Alert severity="info">{hasHistory(form.id) ? "Las horas TEL no se pueden modificar porque la asignatura tiene historial académico." : hasSections(form.id) ? "Las horas TEL no se pueden modificar porque la asignatura tiene secciones asociadas." : "No se pudieron comprobar las condiciones necesarias para editar las horas TEL."}</Alert>
            </>}
            {(!form.id || editField === "prerequisiteIds") && <TextField select label="Prerrequisitos (máximo 3)" value={form.prerequisiteIds} slotProps={{ select: { multiple: true, renderValue: ids => ids.map(prerequisiteName).join(", ") } }} onChange={event => setForm({ ...form, prerequisiteIds: event.target.value })}>
                {subjects.rows.filter(row => String(row.studyPlanId) === String(form.studyPlanId) && row.semester < Number(form.semester) && row.id !== form.id).map(row => <MenuItem disabled={form.prerequisiteIds.length >= 3 && !form.prerequisiteIds.includes(row.id)} key={row.id} value={row.id}>{row.code} — {row.name}</MenuItem>)}
            </TextField>}
        </AdminForm>}
        {remove && <AdminForm title="Eliminar asignatura" submitLabel="Confirmar eliminación" onClose={() => setRemove(null)} onSubmit={async () => { await http.delete(`/subjects/${remove.id}`); saved(); setForm(null); }}>
            <Alert severity="warning">¿Estás segura de que deseas eliminar la asignatura {remove.code} — {remove.name}? Esta acción es irreversible.</Alert>
        </AdminForm>}
    </Stack>;
}

export default AdminSubjects;
