import { useEffect, useState } from "react";
import PropTypes from "prop-types";
import {
    Alert, Box, Button, CircularProgress, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Typography
} from "@mui/material";
import subjectService from "../../services/subject.service";
import { studentErrorMessage } from "../../services/studentAcademic";

function StudentAcademicRecords({ gradesOnly = false }) {
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [retry, setRetry] = useState(0);
    useEffect(() => {
        let active = true;
        setLoading(true);
        setError("");
        subjectService.getCurrentStudentAcademicHistory().then(({ data }) => {
            if (active) setHistory([...data].sort((a, b) =>
                b.academicPeriodCode.localeCompare(a.academicPeriodCode, undefined, { numeric: true }) ||
                a.subjectCode.localeCompare(b.subjectCode)
            ));
        }).catch(err => {
            if (active) setError(studentErrorMessage(err, "No se pudo cargar el historial académico. Reintenta la carga."));
        }).finally(() => {
            if (active) setLoading(false);
        });
        return () => { active = false; };
    }, [retry]);

    const rows = gradesOnly ? history.filter(record => record.finalGrade != null) : history;
    const title = gradesOnly ? "Mis calificaciones" : "Historial académico";
    return <Box>
        <Typography variant="h5" sx={{ fontWeight: 600, mb: 2 }}>{title}</Typography>
        <Typography variant="body2" sx={{ mb: 3, color: "var(--secondary-text-color)" }}>
            {gradesOnly ? "Calificaciones registradas, con su resultado y período académico."
                : "Asignaturas de períodos anteriores y asignaturas que ya tienen una calificación registrada."}
        </Typography>
        {loading ? <CircularProgress aria-label="Cargando registros académicos" /> : error ? <>
            <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
            <Button variant="outlined" onClick={() => setRetry(value => value + 1)}>Reintentar</Button>
        </> : rows.length === 0 ? <Alert severity="info">
            {gradesOnly ? "Todavía no tienes calificaciones registradas." : "Todavía no tienes registros en el historial académico."}
        </Alert> : <TableContainer>
            <Table size="small" aria-label={title}>
                <TableHead><TableRow>
                    {["Código", "Asignatura", "Período académico", "Nota final", "Resultado"].map(label =>
                        <TableCell key={label}>{label}</TableCell>
                    )}
                </TableRow></TableHead>
                <TableBody>{rows.map(record => <TableRow key={record.enrollmentId}>
                    <TableCell>{record.subjectCode}</TableCell>
                    <TableCell>{record.subjectName}</TableCell>
                    <TableCell>{record.academicPeriodCode}</TableCell>
                    <TableCell>{record.finalGrade == null ? "Pendiente" : Number(record.finalGrade).toFixed(1)}</TableCell>
                    <TableCell>{record.result === "APROBADO" ? "Aprobada"
                        : record.result === "REPROBADO" ? "Reprobada" : "Sin calificación registrada"}</TableCell>
                </TableRow>)}</TableBody>
            </Table>
        </TableContainer>}
    </Box>;
}

StudentAcademicRecords.propTypes = { gradesOnly: PropTypes.bool };
export default StudentAcademicRecords;
