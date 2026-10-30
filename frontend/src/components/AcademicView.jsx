import { useEffect, useState } from "react";
import PropTypes from "prop-types";
import { Link as RouterLink } from "react-router-dom";
import {
    Alert, Button, CircularProgress, Paper, Stack, Table, TableBody,
    TableCell, TableContainer, TableHead, TableRow, Typography
} from "@mui/material";
import http from "../http-common";
import careerService from "../services/career.service";

function AcademicView({ title, endpoint, columns, sectionLinks = false }) {
    const [result, setResult] = useState({ endpoint: null, rows: [], error: null });
    useEffect(() => {
        let active = true;
        const request = endpoint === "/careers" ? careerService.getAllCareers() : http.get(endpoint);
        request.then(({ data }) => {
            if (active) setResult({ endpoint, rows: Array.isArray(data) ? data : [data], error: null });
        }).catch(error => {
            const status = error.response?.status;
            const message = {
                401: "La sesión expiró. Vuelve a iniciar sesión.",
                403: "No tienes permiso para consultar esta información.",
                404: "No se encontró el recurso o tu cuenta aún no está vinculada a SIGA."
            }[status] || "No se pudo cargar la información académica.";
            if (active) setResult({ endpoint, rows: [], error: message });
        });
        return () => { active = false; };
    }, [endpoint]);
    return <Stack spacing={2}>
        <Typography variant="h5">{title}</Typography>
        {result.endpoint !== endpoint ? <CircularProgress aria-label="Cargando" /> :
            result.error ? <Alert severity="error">{result.error}</Alert> :
                result.rows.length === 0 ? <Alert severity="info">No hay registros disponibles.</Alert> : (
                    <TableContainer component={Paper}>
                        <Table size="small" aria-label={title}>
                            <TableHead><TableRow>
                                {columns.map(([field, label]) => <TableCell key={field}>{label}</TableCell>)}
                                {sectionLinks && <TableCell>Consultas de sección</TableCell>}
                            </TableRow></TableHead>
                            <TableBody>{result.rows.map((row, index) => (
                                <TableRow key={row.id ?? index}>
                                    {columns.map(([field]) => <TableCell key={field}>
                                        {Array.isArray(row[field]) ? row[field].join(", ") : String(row[field] ?? "—")}
                                    </TableCell>)}
                                    {sectionLinks && <TableCell>
                                        <Button component={RouterLink} to={`/teacher/sections/${row.id}/students`}>Estudiantes</Button>
                                        <Button component={RouterLink} to={`/teacher/sections/${row.id}/grades`}>Notas</Button>
                                    </TableCell>}
                                </TableRow>
                            ))}</TableBody>
                        </Table>
                    </TableContainer>
                )}
    </Stack>;
}

AcademicView.propTypes = {
    title: PropTypes.string.isRequired,
    endpoint: PropTypes.string.isRequired,
    columns: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.string)).isRequired,
    sectionLinks: PropTypes.bool
};
export default AcademicView;
