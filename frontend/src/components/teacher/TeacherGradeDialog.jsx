import { useState } from "react";
import PropTypes from "prop-types";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField, Typography } from "@mui/material";
import http from "../../http-common";
import { formatFinalGrade, parseFinalGrade, teacherErrorMessage } from "../../services/teacherAcademic";
import { teacherContentSx } from "../../styles/teacherContent";

function TeacherGradeDialog({ student, section, onClose, onSaved }) {
    const [value, setValue] = useState(student.grade ? formatFinalGrade(student.grade.finalGrade) : "");
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    const save = async event => {
        event.preventDefault();
        if (saving) return;
        setSaving(true);
        setError("");
        try {
            if (section.periodStatus !== "ABIERTO" || section.status !== "ACTIVA" || !student.enrollmentId) {
                throw new Error("Se requiere una sección activa, una inscripción activa y un período abierto para guardar la nota.");
            }
            const data = {
                enrollmentId: student.grade?.enrollmentId ?? student.enrollmentId,
                finalGrade: parseFinalGrade(value)
            };
            const response = student.grade
                ? await http.put(`/grades/${student.grade.id}`, data)
                : await http.post("/grades", data);
            onSaved(response.data);
            onClose();
        } catch (error) {
            setError(teacherErrorMessage(error));
        } finally {
            setSaving(false);
        }
    };

    return <Dialog open onClose={saving ? undefined : onClose} fullWidth maxWidth="sm" sx={teacherContentSx}>
        <form onSubmit={save}>
            <DialogTitle>{student.grade ? "Editar" : "Registrar"} calificación</DialogTitle>
            <DialogContent><Stack spacing={2} sx={{ pt: 1 }}>
                {error && <Alert severity="error">{error}</Alert>}
                <Typography>{student.firstName} {student.lastName}</Typography>
                <Typography variant="body2">{section.subjectCode} — {section.subjectName} · Sección {section.id} · {section.periodCode}</Typography>
                <TextField autoFocus required disabled={saving} type="text" label="Nota final" value={value} onChange={event => setValue(event.target.value)}
                    slotProps={{ htmlInput: { inputMode: "decimal" } }} helperText="Entre 1,0 y 7,0. Máximo un decimal; puedes usar coma o punto." />
            </Stack></DialogContent>
            <DialogActions>
                <Button disabled={saving} onClick={onClose}>Cancelar</Button>
                <Button disabled={saving} type="submit" variant="contained">{saving ? "Guardando…" : "Guardar"}</Button>
            </DialogActions>
        </form>
    </Dialog>;
}

TeacherGradeDialog.propTypes = {
    student: PropTypes.shape({ enrollmentId: PropTypes.number, firstName: PropTypes.string, lastName: PropTypes.string, grade: PropTypes.object }).isRequired,
    section: PropTypes.object.isRequired, onClose: PropTypes.func.isRequired, onSaved: PropTypes.func.isRequired
};

export default TeacherGradeDialog;
