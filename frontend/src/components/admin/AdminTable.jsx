import PropTypes from "prop-types";
import {
    Alert, Button, CircularProgress, Stack, Table, TableBody,
    TableCell, TableContainer, TableHead, TableRow, Typography
} from "@mui/material";

function AdminTable({ title, records, columns, actions, filtered = false }) {
    if (records.loading) return <CircularProgress aria-label="Cargando" />;
    if (records.error) return <Alert severity="error" action={<Button color="inherit" onClick={records.reload}>Reintentar</Button>}>{records.error}</Alert>;
    if (!records.rows.length && !filtered) return <Alert severity="info">No hay registros disponibles para esta consulta.</Alert>;

    return (
        <Stack spacing={1}>
            {filtered && <Typography variant="body2" role="status">Registros encontrados: {records.rows.length}</Typography>}
            {!records.rows.length ? <Alert severity="info">No se encontraron registros con los filtros seleccionados</Alert> : <TableContainer>
                <Table size="small" aria-label={title}>
                    <TableHead><TableRow>
                        {columns.map(([field, label]) => <TableCell key={field}>{label}</TableCell>)}
                        {actions && <TableCell sx={{ width: 152, minWidth: 152 }}>Acciones</TableCell>}
                    </TableRow></TableHead>
                    <TableBody>{records.rows.map(row => (
                        <TableRow key={row.id ?? row.enrollmentId}>
                            {columns.map(([field, , render]) => <TableCell key={field} sx={{ maxWidth: 320, overflowWrap: "anywhere" }}>
                                {render ? render(row) : Array.isArray(row[field]) ? row[field].join(", ") : String(row[field] ?? "—")}
                            </TableCell>)}
                            {actions && <TableCell sx={{ width: 152, minWidth: 152, whiteSpace: "nowrap" }}><Stack direction="row" spacing={1} sx={{ flexWrap: "nowrap", "& .MuiButton-root": { minWidth: 104, height: 36, flexShrink: 0 } }}>{actions(row)}</Stack></TableCell>}
                        </TableRow>
                    ))}</TableBody>
                </Table>
            </TableContainer>}
        </Stack>
    );
}

AdminTable.propTypes = {
    title: PropTypes.string.isRequired,
    records: PropTypes.shape({ rows: PropTypes.array.isRequired, error: PropTypes.string, loading: PropTypes.bool, reload: PropTypes.func }).isRequired,
    columns: PropTypes.array.isRequired,
    actions: PropTypes.func,
    filtered: PropTypes.bool
};

export default AdminTable;
