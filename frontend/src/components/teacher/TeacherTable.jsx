import PropTypes from "prop-types";
import { Alert, Button, CircularProgress, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from "@mui/material";

function TeacherTable({ title, records, columns, emptyMessage }) {
    if (records.loading) return <CircularProgress aria-label="Cargando" />;
    if (records.error) return <Alert severity="error" action={<Button color="inherit" onClick={records.reload}>Reintentar</Button>}>{records.error}</Alert>;
    if (!records.rows.length) return <Alert severity="info">{emptyMessage}</Alert>;

    return <TableContainer>
        <Table size="small" aria-label={title}>
            <TableHead><TableRow>{columns.map(([field, label]) => <TableCell key={field}>{label}</TableCell>)}</TableRow></TableHead>
            <TableBody>{records.rows.map(row => <TableRow key={row.enrollmentId ?? row.id}>
                {columns.map(([field, , render]) => <TableCell key={field} sx={{ maxWidth: 320, overflowWrap: "anywhere" }}>
                    {render ? render(row) : Array.isArray(row[field]) ? row[field].join(", ") : String(row[field] ?? "—")}
                </TableCell>)}
            </TableRow>)}</TableBody>
        </Table>
    </TableContainer>;
}

TeacherTable.propTypes = {
    title: PropTypes.string.isRequired, records: PropTypes.shape({ rows: PropTypes.array.isRequired, loading: PropTypes.bool, error: PropTypes.string, reload: PropTypes.func }).isRequired,
    columns: PropTypes.array.isRequired, emptyMessage: PropTypes.string.isRequired
};

export default TeacherTable;
