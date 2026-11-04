import { useState } from "react";
import PropTypes from "prop-types";
import { Alert, Button, MenuItem, TextField } from "@mui/material";
import { AdminForm } from "./AdminDialog";
import { useAdminRecords } from "../../services/adminAcademic";
import http from "../../http-common";

function StudentMatriculationDialog({ student, onClose, onSaved }) {
    // get academic periods and the student's external matriculations
    // GET /api/academic-periods
    const periods = useAdminRecords("/academic-periods");
    // GET /api/period-matriculations/student/{studentId}
    const matriculations = useAdminRecords(`/period-matriculations/student/${student.id}`);
    const [periodId, setPeriodId] = useState("");
    const [selection, setSelection] = useState(null);
    const current = matriculations.rows.some(row => String(row.academicPeriodId) === String(periodId));
    const value = selection ?? (current ? "yes" : "no");
    const loading = periods.loading || matriculations.loading;
    const failed = Boolean(periods.error || matriculations.error);
    const openPeriods = periods.rows.filter(row => row.status === "ABIERTO");

    return <AdminForm title={`Matrícula de ${student.firstName} ${student.lastName}`} onClose={onClose}
        submitDisabled={loading || failed || !openPeriods.some(row => String(row.id) === String(periodId))}
        onSubmit={async () => {
            // record the external status (PATCH /api/period-matriculations/student/{studentId}/academic-period/{periodId})
            await http.patch(`/period-matriculations/student/${student.id}/academic-period/${periodId}`, null, {
                params: { matriculated: value === "yes" }
            });
            onSaved();
        }}>
        <Alert severity="info">Registra si el estudiante está matriculado externamente en el período seleccionado.</Alert>
        {[periods, matriculations].filter(records => records.error).map((records, index) =>
            <Alert key={index} severity="error" action={<Button onClick={records.reload}>Reintentar</Button>}>{records.error}</Alert>)}
        {!loading && !failed && !openPeriods.length && <Alert severity="info">No hay períodos abiertos para actualizar la matrícula.</Alert>}
        <TextField required select label="Período de matrícula" value={periodId} disabled={loading || failed}
            onChange={event => { setPeriodId(event.target.value); setSelection(null); }}>
            {openPeriods.map(row => <MenuItem key={row.id} value={row.id}>{row.code}</MenuItem>)}
        </TextField>
        <TextField required select label="Matriculado" value={periodId ? value : ""} disabled={!periodId || loading || failed}
            onChange={event => setSelection(event.target.value)}>
            <MenuItem value="yes">Sí</MenuItem>
            <MenuItem value="no">No</MenuItem>
        </TextField>
        <Alert severity="info">Para cambiar a No, el estudiante no debe tener asignaturas inscritas ni una selección confirmada en ese período.</Alert>
    </AdminForm>;
}

StudentMatriculationDialog.propTypes = {
    student: PropTypes.shape({ id: PropTypes.number.isRequired, firstName: PropTypes.string, lastName: PropTypes.string }).isRequired,
    onClose: PropTypes.func.isRequired,
    onSaved: PropTypes.func.isRequired
};

export default StudentMatriculationDialog;
