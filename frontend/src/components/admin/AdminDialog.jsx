import { Children, cloneElement, isValidElement, useState } from "react";
import PropTypes from "prop-types";
import {
    Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, Stack, TextField
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
    const [fieldErrors, setFieldErrors] = useState({});
    const choosing = editOptions && !selectedOption;

    const visitFields = (nodes, visit) => Children.map(nodes, child => {
        if (!isValidElement(child)) return child;
        if (child.type === TextField) return visit(child);
        return child.props.children ? cloneElement(child, {}, visitFields(child.props.children, visit)) : child;
    });

    // Validates enabled fields, including inputs inside nested form elements
    const validateFields = () => {
        const errors = {};
        visitFields(children, child => {
            const { label, value, required, disabled, type, slotProps } = child.props;
            if (disabled) return child;
            const input = slotProps?.htmlInput || {};
            const text = String(value ?? "");
            if (required && (!text.trim() || (Array.isArray(value) && !value.length))) {
                errors[label] = "El campo es obligatorio";
            } else if (type === "number" && text) {
                const number = Number(text);
                if (!/^-?[0-9]+$/.test(text) || !Number.isSafeInteger(number) || (input.min != null && number < input.min) || (input.max != null && number > input.max)) {
                    errors[label] = `Ingrese un número entero${input.min != null ? ` desde ${input.min}` : ""}${input.max != null ? ` hasta ${input.max}` : ""}`;
                }
            } else if (input.maxLength && text.length > input.maxLength) {
                errors[label] = `El máximo es ${input.maxLength} caracteres`;
            }
            return child;
        });
        setFieldErrors(errors);
        if (Object.keys(errors).length) {
            setError(Object.entries(errors).map(([label, message]) => `${label}: ${message}`).join(". "));
            return false;
        }
        return true;
    };

    const selectOption = async value => {
        if (saving) return;
        setError("");
        setFieldErrors({});
        setSaving(true);
        try {
            // Reloads the record before choosing the field to edit
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
        if (!validateFields()) return;
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
        <form noValidate onSubmit={submit}>
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
                        <Stack spacing={2}>{visitFields(children, child => fieldErrors[child.props.label] ? cloneElement(child, {
                            error: true, helperText: fieldErrors[child.props.label],
                            onChange: event => {
                                setFieldErrors(errors => ({ ...errors, [child.props.label]: "" }));
                                child.props.onChange?.(event);
                            }
                        }) : child)}</Stack>
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
