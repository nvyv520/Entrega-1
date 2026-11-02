import { useEffect, useState } from "react";
import PropTypes from "prop-types";
import {
    Alert, Box, Button, CircularProgress, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Typography
} from "@mui/material";
import subjectService from "../../services/subject.service";
import { latestAcademicPeriod, studentErrorMessage } from "../../services/studentAcademic";
import enrollmentService from "../../services/enrollment.service";
import http from "../../http-common";

function StudentAcademicRecords({ gradesOnly = false }) {
    const [records, setRecords] = useState([]);
    const [periodCode, setPeriodCode] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [retry, setRetry] = useState(0);
    useEffect(() => {
        let active = true;
        setLoading(true);
        setError("");
        const loadRecords = async () => {
            try {
                const periodsResponse = await http.get("/academic-periods");
                let rows = [];
                let code = "";
                if (gradesOnly) {
                    const period = latestAcademicPeriod(periodsResponse.data.filter(item => item.status === "ABIERTO"));
                    code = period?.code || "";
                    if (period) {
                        const [enrollmentsResponse, gradesResponse, sectionsResponse, subjectsResponse] = await Promise.all([
                            enrollmentService.getCurrentEnrollments(), http.get("/grades/me"),
                            enrollmentService.getSectionsByAcademicPeriod(period.id), http.get("/subjects")
                        ]);
                        rows = enrollmentsResponse.data.filter(enrollment => enrollment.status === "ACTIVO" && Number(enrollment.academicPeriodId) === Number(period.id)).map(enrollment => {
                            const section = sectionsResponse.data.find(section => Number(section.id) === Number(enrollment.sectionId));
                            const subject = subjectsResponse.data.find(subject => Number(subject.id) === Number(section?.subjectId));
                            if (!section || !subject) throw new Error("Información de asignatura incompleta");
                            const grade = gradesResponse.data.find(grade => Number(grade.enrollmentId) === Number(enrollment.id));
                            return {
                                enrollmentId: enrollment.id, subjectCode: subject.code, subjectName: subject.name,
                                academicPeriodCode: period.code, finalGrade: grade?.finalGrade ?? null, result: grade?.result ?? null
                            };
                        }).sort((first, second) => first.subjectCode.localeCompare(second.subjectCode));
                    }
                } else {
                    const historyResponse = await subjectService.getCurrentStudentAcademicHistory();
                    const closedPeriods = new Set(periodsResponse.data.filter(period => period.status === "CERRADO").map(period => Number(period.id)));
                    rows = historyResponse.data.filter(record => closedPeriods.has(Number(record.academicPeriodId)) && record.finalGrade != null)
                        .sort((first, second) => second.academicPeriodCode.localeCompare(first.academicPeriodCode, undefined, { numeric: true })
                            || first.subjectCode.localeCompare(second.subjectCode) || Number(first.enrollmentId) - Number(second.enrollmentId));
                }
                if (active) { setRecords(rows); setPeriodCode(code); }
            } catch (error) {
                if (active) setError(studentErrorMessage(error, "No se pudieron cargar los registros académicos. Reintenta la carga."));
            } finally {
                if (active) setLoading(false);
            }
        };
        loadRecords();
        return () => { active = false; };
    }, [gradesOnly, retry]);

    const title = gradesOnly ? "Mis calificaciones" : "Historial académico";
    return <Box>
        <Typography variant="h5" sx={{ fontWeight: 600, mb: 2 }}>{title}</Typography>
        <Typography variant="body2" sx={{ mb: 3, color: "var(--secondary-text-color)" }}>
            {gradesOnly ? (periodCode ? `Período académico actual: ${periodCode}` : "Calificaciones del período académico actual.")
                : "Asignaturas y resultados consolidados en períodos cerrados."}
        </Typography>
        {loading ? <CircularProgress aria-label="Cargando registros académicos" /> : error ? <>
            <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
            <Button variant="outlined" onClick={() => setRetry(value => value + 1)}>Reintentar</Button>
        </> : records.length === 0 ? <Alert severity="info">
            {gradesOnly ? "No tienes inscripciones en el período académico actual." : "Todavía no tienes registros en el historial académico."}
        </Alert> : <TableContainer>
            <Table size="small" aria-label={title}>
                <TableHead><TableRow>
                    {["Código", "Asignatura", "Período académico", "Nota final", "Resultado"].map(label =>
                        <TableCell key={label}>{label}</TableCell>
                    )}
                </TableRow></TableHead>
                <TableBody>{records.map(record => <TableRow key={record.enrollmentId}>
                    <TableCell>{record.subjectCode}</TableCell>
                    <TableCell>{record.subjectName}</TableCell>
                    <TableCell>{record.academicPeriodCode}</TableCell>
                    <TableCell>{record.finalGrade == null ? "Pendiente" : Number(record.finalGrade).toFixed(1)}</TableCell>
                    <TableCell>{["APROBADO", "REPROBADO"].includes(record.result) ? record.result : "Pendiente"}</TableCell>
                </TableRow>)}</TableBody>
            </Table>
        </TableContainer>}
    </Box>;
}

StudentAcademicRecords.propTypes = { gradesOnly: PropTypes.bool };
export default StudentAcademicRecords;
