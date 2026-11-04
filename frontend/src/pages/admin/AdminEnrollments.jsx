import { useState } from "react";
import { Alert, Button, MenuItem, Stack, TextField, Typography } from "@mui/material";
import AdminTable from "../../components/admin/AdminTable";
import { filterValues, filteredRecords, matchesFilter, useAdminRecords } from "../../services/adminAcademic";

// consult enrollments; filters are applied to the loaded records
function AdminEnrollments() {
    // GET /api/academic-periods
    const periods = useAdminRecords("/academic-periods");
    // GET /api/students
    const students = useAdminRecords("/students");
    // GET /api/careers
    const careers = useAdminRecords("/careers");
    // GET /api/sections
    const sections = useAdminRecords("/sections");
    // GET /api/subjects
    const subjects = useAdminRecords("/subjects");
    const [periodId, setPeriodId] = useState("");
    const [careerId, setCareerId] = useState("");
    const [status, setStatus] = useState("");
    const [studentId, setStudentId] = useState("");
    const [sectionId, setSectionId] = useState("");
    // GET /api/enrollments
    const enrollments = useAdminRecords("/enrollments");
    const studentName = id => {
        const row = students.rows.find(student => student.id === id);
        return row ? `${row.rut} — ${row.firstName} ${row.lastName}` : `ID ${id}`;
    };
    const sectionName = row => {
        const subject = subjects.rows.find(subject => subject.id === row.subjectId);
        const period = periods.rows.find(period => period.id === row.academicPeriodId);
        return `${row.code} — ${subject?.name || "Asignatura no disponible"} — ${period?.code || "Período no disponible"}`;
    };
    const rows = enrollments.rows.filter(row => matchesFilter(row.academicPeriodId, periodId) && matchesFilter(row.studentId, studentId)
        && matchesFilter(row.sectionId, sectionId) && matchesFilter(row.status, status)
        && matchesFilter(students.rows.find(student => student.id === row.studentId)?.careerId, careerId));
    // Requires student records when the career filter is active
    const dependencies = careerId ? [students] : [];

    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Inscripciones</Typography>
        {[periods, careers, students, sections, subjects].filter(records => records.error).map((records, index) => <Alert key={index} severity="error" action={<Button onClick={records.reload}>Reintentar</Button>}>{records.error}</Alert>)}
        <Stack direction="row" spacing={1.5} useFlexGap sx={{ flexWrap: "wrap", alignItems: "center" }}>
            <TextField size="small" select label="Período" value={periodId} onChange={event => { setPeriodId(event.target.value); setSectionId(""); }} sx={{ width: 220 }}>
                <MenuItem value="">Todos</MenuItem>{periods.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.status}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Estudiante" value={studentId} onChange={event => setStudentId(event.target.value)} sx={{ width: 330 }}>
                <MenuItem value="">Todos</MenuItem>{students.rows.filter(row => matchesFilter(row.careerId, careerId)).map(row => <MenuItem key={row.id} value={row.id}>{studentName(row.id)}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Sección" value={sectionId} onChange={event => setSectionId(event.target.value)} sx={{ width: 330 }}>
                <MenuItem value="">Todas</MenuItem>{sections.rows.filter(row => !periodId || String(row.academicPeriodId) === String(periodId)).map(row => <MenuItem key={row.id} value={row.id}>{sectionName(row)}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Carrera" value={careerId} onChange={event => { const value = event.target.value; setCareerId(value); if (value && !students.rows.some(row => String(row.id) === String(studentId) && String(row.careerId) === String(value))) setStudentId(""); }} sx={{ width: 180 }} disabled={careers.loading || Boolean(careers.error)}>
                <MenuItem value="">Todas</MenuItem>
                {careers.rows.map(row => <MenuItem key={row.id} value={row.id}>{row.code} — {row.name}</MenuItem>)}
            </TextField>
            <TextField size="small" select label="Estado de inscripción" value={status} onChange={event => setStatus(event.target.value)} sx={{ width: 210 }}>
                <MenuItem value="">Todos</MenuItem>
                {filterValues(enrollments.rows, "status", status).map(value => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <Button size="small" onClick={() => { setPeriodId(""); setCareerId(""); setStudentId(""); setSectionId(""); setStatus(""); }}>Limpiar filtros</Button>
        </Stack>
        <AdminTable title="Inscripciones" filtered records={filteredRecords(enrollments, rows, dependencies)} columns={[
            ["studentId", "Estudiante", row => studentName(row.studentId)],
            ["subjectId", "Asignatura", row => {
                const section = sections.rows.find(section => section.id === row.sectionId);
                const subject = subjects.rows.find(subject => subject.id === section?.subjectId);
                return subject ? `${subject.code} — ${subject.name}` : "No disponible";
            }],
            ["sectionId", "Sección", row => sections.rows.find(section => section.id === row.sectionId)?.code || "No disponible"], ["academicPeriodId", "Período", row => periods.rows.find(period => period.id === row.academicPeriodId)?.code || "No disponible"],
            ["enrollmentDate", "Fecha"], ["status", "Estado"], ["confirmed", "Confirmación", row => row.confirmed ? "Confirmada" : "Provisional"]
        ]} />
    </Stack>;
}

export default AdminEnrollments;
