import { useState } from "react";
import PropTypes from "prop-types";
import {
    Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, Stack
} from "@mui/material";
import { adminErrorMessage, useAdminRecords } from "../../services/adminAcademic";
import { adminContentSx } from "../../styles/adminContent";
import AdminTable from "./AdminTable";
import http from "../../http-common";

// Mounted only while open, so each operation starts with clean errors and state.
export function AdminForm({ title, children, onClose, onSubmit, submitLabel = "Guardar", editOptions, selectedOption, onSelectOption, recordEndpoint, onLoadRecord, onRemove, removeDisabled = false, readOnly = false, submitDisabled = false, editNotice = "" }) {
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState(false);
    const choosing = editOptions && !selectedOption;

    const selectOption = async value => {
        if (saving) return;
        setError("");
        setSaving(true);
        try {
            if (recordEndpoint) {
                const { data } = await http.get(recordEndpoint);
                onLoadRecord(data);
            }
            onSelectOption(value);
        } catch (error) {
            setError(adminErrorMessage(error));
        } finally {
            setSaving(false);
        }
    };

    const submit = async event => {
        event.preventDefault();
        if (saving || choosing || success || readOnly || submitDisabled) return;
        setSaving(true);
        setError("");
        try {
            await onSubmit();
            if (editOptions) setSuccess(true);
            else onClose();
        } catch (error) {
            setError(adminErrorMessage(error));
        } finally {
            setSaving(false);
        }
    };

    return <Dialog open onClose={saving ? undefined : onClose} fullWidth maxWidth="md" sx={adminContentSx}>
        <form onSubmit={submit}>
            <DialogTitle>{choosing && !success ? "¿Qué deseas hacer?" : title}</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ pt: 1 }}>
                    {error && <Alert severity="error">{error}</Alert>}
                    {success ? <Alert severity="success">Cambios guardados correctamente.</Alert> : choosing ? <>
                        {editNotice && <Alert severity="info">{editNotice}</Alert>}
                        {editOptions.map(option => (
                            <Button key={option.value} variant="outlined" disabled={saving || option.disabled} onClick={() => selectOption(option.value)}>{option.label}</Button>
                        ))}
                        {onRemove && <>
                            <Divider />
                            <Button color="error" variant="outlined" disabled={saving || removeDisabled} onClick={onRemove}>Eliminar registro</Button>
                        </>}
                    </> : <fieldset disabled={saving} style={{ border: 0, margin: 0, padding: 0 }}>
                        <Stack spacing={2}>{children}</Stack>
                    </fieldset>}
                </Stack>
            </DialogContent>
            <DialogActions>
                {editOptions && selectedOption && !success
                    ? <Button disabled={saving} onClick={() => { setError(""); onSelectOption(""); }}>Volver</Button>
                    : <Button disabled={saving} onClick={onClose}>{success ? "Cerrar" : "Cancelar"}</Button>}
                {!choosing && !success && !readOnly && <Button disabled={saving || submitDisabled} type="submit" color={submitLabel === "Confirmar eliminación" ? "error" : "primary"} variant="contained">{saving ? "Guardando…" : submitLabel}</Button>}
            </DialogActions>
        </form>
    </Dialog>;
}

AdminForm.propTypes = {
    title: PropTypes.string.isRequired, children: PropTypes.node.isRequired,
    onClose: PropTypes.func.isRequired, onSubmit: PropTypes.func.isRequired, submitLabel: PropTypes.string,
    editOptions: PropTypes.arrayOf(PropTypes.shape({ value: PropTypes.string.isRequired, label: PropTypes.string.isRequired, disabled: PropTypes.bool })),
    selectedOption: PropTypes.string, onSelectOption: PropTypes.func,
    recordEndpoint: PropTypes.string, onLoadRecord: PropTypes.func,
    onRemove: PropTypes.func, removeDisabled: PropTypes.bool, readOnly: PropTypes.bool, submitDisabled: PropTypes.bool, editNotice: PropTypes.string
};

export function AdminQuery({ title, endpoint, columns, onClose, sortRows }) {
    const records = useAdminRecords(endpoint);
    return <Dialog open onClose={onClose} fullWidth maxWidth="lg" sx={adminContentSx}>
        <DialogTitle>{title}</DialogTitle>
        <DialogContent><AdminTable title={title} records={sortRows ? { ...records, rows: [...records.rows].sort(sortRows) } : records} columns={columns} /></DialogContent>
        <DialogActions><Button onClick={onClose}>Cerrar</Button></DialogActions>
    </Dialog>;
}

AdminQuery.propTypes = {
    title: PropTypes.string.isRequired, endpoint: PropTypes.string.isRequired,
    columns: PropTypes.array.isRequired, onClose: PropTypes.func.isRequired, sortRows: PropTypes.func
};
