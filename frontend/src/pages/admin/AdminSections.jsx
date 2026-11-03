import { useState } from "react";
import { Alert, Button, Checkbox, MenuItem, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from "@mui/material";
import http from "../../http-common";
import AdminTable from "../../components/admin/AdminTable";
import { AdminForm, AdminQuery } from "../../components/admin/AdminDialog";
import { filterValues, filteredRecords, matchesFilter, saveAdminFields, useAdminRecords } from "../../services/adminAcademic";

const days = [["L", "Lunes"], ["M", "Martes"], ["W", "Miércoles"], ["J", "Jueves"], ["V", "Viernes"]];
const blocks = [1, 2, 3, 4, 5, 6];

function AdminSections() {
    const [periodId, setPeriodId] = useState("");
    const [careerId, setCareerId] = useState("");
    const [studyPlanId, setStudyPlanId] = useState("");
    const [semester, setSemester] = useState("");
    const [teacherId, setTeacherId] = useState("");
    const [status, setStatus] = useState("");
    const periods = useAdminRecords("/academic-periods");
    const sections = useAdminRecords("/sections");
    const subjects = useAdminRecords("/subjects");
    const careers = useAdminRecords("/careers");
    const plans = useAdminRecords("/study-plans");
    const teachers = useAdminRecords("/teachers");
    const students = useAdminRecords("/students");
    const enrollments = useAdminRecords("/enrollments");
    const [form, setForm] = useState(null);
    const [editField, setEditField] = useState("");
    const [remove, setRemove] = useState(null);
    const [query, setQuery] = useState(null);
    const [message, setMessage] = useState("");
    const [scheduleError, setScheduleError] = useState("");
    const subjectName = id => {
        const subject = subjects.rows.find(row => row.id === id);
        return subject ? `${subject.code} — ${subject.name}` : "No disponible";
    };
    const studentName = id => {
        const student = students.rows.find(row => row.id === id);
        return student ? `${student.rut} — ${student.firstName} ${student.lastName}` : `ID ${id}`;
    };
    const periodOpen = id => periods.rows.some(row => row.id === id && row.status === "ABIERTO");
    const selectorsReady = [periods, subjects, teachers].every(records => !records.loading && !records.error);
    // Includes cancelled enrollments when checking whether a section can change
    const canEdit = row => selectorsReady && periodOpen(row.academicPeriodId) && !enrollments.loading && !enrollments.error && !enrollments.rows.some(enrollment => enrollment.sectionId === row.id);
    const canRemove = row => !periods.loading && !periods.error && periodOpen(row.academicPeriodId) && !enrollments.loading && !enrollments.error && !enrollments.rows.some(enrollment => enrollment.sectionId === row.id);

    const rows = sections.rows.filter(row => {
        // Resolves the subject and plan for the related academic filters
        const subject = subjects.rows.find(subject => subject.id === row.subjectId);
        const plan = plans.rows.find(plan => plan.id === subject?.studyPlanId);
        return matchesFilter(row.academicPeriodId, periodId) && matchesFilter(row.teacherId, teacherId)
            && matchesFilter(row.status, status)
            && matchesFilter(subject?.studyPlanId, studyPlanId) && matchesFilter(subject?.semester, semester) && matchesFilter(plan?.careerId, careerId);
    });
    const dependencies = careerId ? [subjects, plans] : studyPlanId || semester ? [subjects] : [];

    // Recalculates the required schedule whenever the selected subject changes.
    const selectedSubject = subjects.rows.find(row => String(row.id) === String(form?.subjectId));
    const requiredHours = selectedSubject ? Number(selectedSubject.theoryHours) + Number(selectedSubject.exerciseHours) + Number(selectedSubject.laboratoryHours) : null;
    const requiredBlocks = requiredHours !== null && requiredHours % 2 === 0 ? requiredHours / 2 : null;
    const selectedHours = (form?.blocks.length || 0) * 2;
    const scheduleComplete = requiredBlocks !== null && requiredBlocks > 0 && selectedHours === requiredHours;

    const changeScheduleBlock = (value, checked) => {
        if (!checked) {
            setForm({ ...form, blocks: form.blocks.filter(item => item !== value) });
            setScheduleError("");
            return;
        }

        // Keeps the previous selection when another block would exceed the TEL.
        if (requiredBlocks === null || selectedHours + 2 > requiredHours) {
            setScheduleError("No puedes superar las horas TEL de la asignatura. Desmarca un bloque antes de seleccionar otro.");
            return;
        }

        setForm({ ...form, blocks: [...form.blocks, value] });
        setScheduleError("");
    };

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Secciones</Typography>
        {message && <Alert severity="success" onClose={() => setMessage("")}>{message}</Alert>}
        {[periods, careers, plans, subjects, teachers, students, enrollments].filter(records => records.error).map((records, index) => <Alert key={index} severity="error" action={<Button onClick={records.reload}>Reintentar</Button>}>{records.error}</Alert>)}
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: "wrap", alignItems: "center" }}>
            <TextField size="small" select label="Período académico" value={periodId} onChange={event => setPeriodId(event.target.value)} sx={{ width: 300 }}>
                <MenuItem value="">Todos</MenuItem>
                {periods.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.status}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Carrera" value={careerId} onChange={event => { const value = event.target.value; setCareerId(value); if (value && !plans.rows.some(row => String(row.id) === String(studyPlanId) && String(row.careerId) === String(value))) setStudyPlanId(""); }} sx={{ width: 180 }} disabled={careers.loading || Boolean(careers.error)}>
                <MenuItem value="">Todas</MenuItem>
                {careers.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.name}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Plan de estudio" value={studyPlanId} onChange={event => setStudyPlanId(event.target.value)} sx={{ width: 180 }} disabled={plans.loading || Boolean(plans.error)}>
                <MenuItem value="">Todos</MenuItem>
                {plans.rows.filter(row => matchesFilter(row.careerId, careerId)).map(row => <MenuItem key={row.id} value={row.id}>{row.identifier} ({row.careerCode})</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Semestre curricular" value={semester} onChange={event => setSemester(event.target.value)} sx={{ width: 190 }} disabled={subjects.loading || Boolean(subjects.error)}>
                <MenuItem value="">Todos</MenuItem>
                {filterValues(subjects.rows, "semester", semester).filter(value => Number(value) >= 1 && Number(value) <= 4).map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Docente responsable" value={teacherId} onChange={event => setTeacherId(event.target.value)} sx={{ width: 240 }} disabled={teachers.loading || Boolean(teachers.error)}>
                <MenuItem value="">Todos</MenuItem>
                {teachers.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.firstName} {row.lastName} — {row.rut}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Estado de sección" value={status} onChange={event => setStatus(event.target.value)} sx={{ width: 190 }}>
                <MenuItem value="">Todos</MenuItem>
                {["ACTIVA", "INACTIVA"].map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <Button size="small" onClick={() => { setPeriodId(""); setCareerId(""); setStudyPlanId(""); setSemester(""); setTeacherId(""); setStatus(""); }}>Limpiar filtros</Button>
            <Button variant="contained" disabled={!selectorsReady} onClick={() => { setScheduleError(""); setForm({ subjectId: "", academicPeriodId: periods.rows.some(row => String(row.id) === String(periodId) && row.status === "ABIERTO") ? periodId : "", teacherId: "", capacity: "", blocks: [] }); }}>Crear sección</Button>
        </Stack>
        <AdminTable title="Secciones" filtered records={filteredRecords(sections, rows, dependencies)} columns={[
            ["code", "Sección"], ["subjectId", "Asignatura", row => subjectName(row.subjectId)], ["academicPeriodId", "Período", row => periods.rows.find(period => period.id === row.academicPeriodId)?.code || "No disponible"],
            ["teacherName", "Docente"], ["capacity", "Capacidad"], ["enrolledStudents", "Cupos ocupados"], ["available", "Disponibles", row => row.capacity - row.enrolledStudents], ["blocks", "Bloques"], ["status", "Estado"],
            ["students", "Inscritos", row => <Button onClick={() => setQuery({ title: `Selecciones e inscripciones de la sección ${row.code}`, endpoint: `/enrollments/section/${row.id}`, columns: [["studentId", "Estudiante", item => studentName(item.studentId)], ["enrollmentDate", "Fecha"], ["status", "Estado"], ["confirmed", "Confirmación", item => item.confirmed ? "Confirmada" : "Provisional"]] })}>Ver</Button>]
        ]} actions={row => <>
            <Button onClick={() => { setEditField(""); setScheduleError(""); setForm({ ...row }); }}>Editar</Button>
        </>} />
        <Alert severity="info">Solo se editan secciones de períodos abiertos sin inscripciones, incluso canceladas. La asignatura y el período no se pueden cambiar. No existe un endpoint para cambiar manualmente el estado.</Alert>
        {form && <AdminForm title={`${form.id ? "Editar" : "Crear"} sección`} onClose={() => setForm(null)} onSubmit={async () => {
            if (!form.blocks.length) throw new Error("Selecciona al menos un bloque horario.");
            if (!selectedSubject || !selectorsReady) throw new Error("No se pudieron consultar las horas TEL de la asignatura. Recarga los datos antes de guardar.");
            if (requiredBlocks === null) throw new Error(`El total de ${requiredHours} horas TEL no puede representarse exactamente mediante bloques completos de 2 horas.`);
            if (!scheduleComplete) throw new Error(`Selecciona exactamente ${requiredBlocks} bloques de 2 horas para completar las ${requiredHours} horas TEL.`);
            const data = { subjectId: Number(form.subjectId), academicPeriodId: Number(form.academicPeriodId), teacherId: Number(form.teacherId), capacity: Number(form.capacity), blocks: form.blocks };
            if (form.id) await saveAdminFields("/sections", form.id, data, ["subjectId", "academicPeriodId", "teacherId", "capacity", "blocks"], [editField]);
            else await http.post("/sections", data);
            sections.reload();
            setMessage("Sección guardada correctamente.");
        }} editOptions={form.id ? [["teacherId", "Docente"], ["capacity", "Capacidad"], ["blocks", "Bloques horarios"]].map(([value, label]) => ({ value, label, disabled: !canEdit(form) })) : undefined}
            selectedOption={editField} onSelectOption={setEditField} recordEndpoint={form.id ? `/sections/${form.id}` : undefined} onLoadRecord={setForm} onRemove={() => setRemove(form)} removeDisabled={!canRemove(form)} submitDisabled={!selectorsReady || !scheduleComplete}>
            {!form.id && <TextField required select label="Asignatura" value={form.subjectId} onChange={event => { setForm({ ...form, subjectId: event.target.value }); setScheduleError(""); }}>
                {subjects.rows.map(row => { const plan = plans.rows.find(plan => plan.id === row.studyPlanId); return <MenuItem key={row.id} value={row.id}>{row.code} — {row.name} (plan {plan ? `${plan.identifier} (${plan.careerCode})` : "No disponible"})</MenuItem>; })}
            </TextField>}
            {!form.id && <TextField required select label="Período académico" value={form.academicPeriodId} onChange={event => setForm({ ...form, academicPeriodId: event.target.value })}>
                {periods.rows.filter(row => row.status === "ABIERTO").map(row => <MenuItem key={row.id} value={row.id}>{row.code}</MenuItem>)}
            </TextField>}
            {(!form.id || editField === "teacherId") && <TextField required select label="Docente" value={form.teacherId} onChange={event => setForm({ ...form, teacherId: event.target.value })}>
                {teachers.rows.filter(row => row.status === "ACTIVO").map(row => <MenuItem key={row.id} value={row.id}>{row.firstName} {row.lastName} — {row.rut}</MenuItem>)}
            </TextField>}
            {(!form.id || editField === "capacity") && <TextField required type="number" label="Capacidad" value={form.capacity} slotProps={{ htmlInput: { min: 1, max: 60, step: 1 } }} onChange={event => setForm({ ...form, capacity: event.target.value })} />}
            {(!form.id || editField === "blocks") && <>
            <Typography variant="h6">Bloques de horario</Typography>
            <Alert severity="info">Cada bloque equivale a 2 horas. El backend valida las horas TEL, los cupos y los topes de horario del docente.</Alert>
            {selectedSubject ? <>
                <Typography>Horas seleccionadas: {selectedHours} / {requiredHours}</Typography>
                {requiredBlocks !== null && <Typography>Bloques seleccionados: {form.blocks.length} / {requiredBlocks}</Typography>}
                {requiredBlocks === null && <Alert severity="error">El total de {requiredHours} horas TEL no puede representarse exactamente mediante bloques completos de 2 horas. No se puede guardar este horario.</Alert>}
                {requiredHours === 0 && <Alert severity="warning">La sección requiere al menos un bloque horario. No se puede asignar un horario a una asignatura con 0 horas TEL.</Alert>}
                {selectedHours < requiredHours && <Typography>Faltan {requiredHours - selectedHours} horas{requiredBlocks !== null ? ` (${requiredBlocks - form.blocks.length} bloques)` : ""} para completar el horario.</Typography>}
                {selectedHours > requiredHours && <Alert severity="error">La selección conservada supera las horas de esta asignatura. Desmarca bloques antes de guardar.</Alert>}
                {scheduleComplete && <Alert severity="success">El horario completa las horas TEL requeridas.</Alert>}
            </> : <Alert severity="info">Selecciona una asignatura para consultar las horas requeridas.</Alert>}
            {scheduleError && <Alert severity="error">{scheduleError}</Alert>}
            <TableContainer><Table size="small" aria-label="Seleccionar horario semanal">
                <TableHead><TableRow><TableCell>Bloque</TableCell>{days.map(([code, label]) => <TableCell key={code} align="center">{label}</TableCell>)}</TableRow></TableHead>
                <TableBody>{blocks.map(block => <TableRow key={block}><TableCell>{block}</TableCell>{days.map(([code, label]) => {
                    const value = `${code}${block}`;
                    return <TableCell key={code} align="center"><Checkbox checked={form.blocks.includes(value)} disabled={!form.blocks.includes(value) && (!selectorsReady || requiredBlocks === null || requiredBlocks === 0)} slotProps={{ input: { "aria-label": `${label}, bloque ${block}` } }} onChange={event => changeScheduleBlock(value, event.target.checked)} /></TableCell>;
                })}</TableRow>)}</TableBody>
            </Table></TableContainer>
            </>}
        </AdminForm>}
        {remove && <AdminForm title="Eliminar sección" submitLabel="Confirmar eliminación" onClose={() => setRemove(null)} onSubmit={async () => { await http.delete(`/sections/${remove.id}`); sections.reload(); setMessage("Sección eliminada correctamente."); setForm(null); }}>
            <Alert severity="warning">¿Estás segura de que deseas eliminar la sección {remove.code} de {subjectName(remove.subjectId)}? Esta acción es irreversible.</Alert>
        </AdminForm>}
        {query && <AdminQuery {...query} onClose={() => setQuery(null)} />}
    </Stack>;
}

export default AdminSections;
