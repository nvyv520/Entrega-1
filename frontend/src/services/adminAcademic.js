import { useEffect, useState } from "react";
import http from "../http-common";
import careerService from "./career.service";

export function adminErrorMessage(error) {
    const data = error.response?.data;
    const reason = typeof data === "string" ? data : data?.detail || data?.message;
    if (error.config?.method === "post" && /^\/(students|teachers)\/?$/.test(error.config?.url || "")
        && (!error.response || error.response.status >= 500)) {
        return `${reason || "No se pudo confirmar el resultado del registro."} Revisa el registro y la cuenta en Keycloak antes de reintentar; puede requerir conciliación manual.`;
    }
    if (typeof reason === "string" && reason.trim()) return reason;
    if (!error.isAxiosError && error.message) return error.message;
    return {
        401: "La sesión expiró. Vuelve a iniciar sesión.",
        403: "No tienes permiso para realizar esta operación.",
        404: "No se encontró el recurso solicitado.",
        400: "Revisa los datos ingresados.",
        409: "La operación no está permitida por las reglas académicas."
    }[error.response?.status] || "No se pudo completar la solicitud. Intenta nuevamente.";
}

// Shared loading and retry behavior for administrative lists and selectors.
export function useAdminRecords(endpoint) {
    const [result, setResult] = useState({ endpoint: null, rows: [], error: "", loading: true });
    const [version, setVersion] = useState(0);

    useEffect(() => {
        let active = true;
        if (!endpoint) return;
        setResult({ endpoint, rows: [], error: "", loading: true });
        const request = endpoint === "/careers" ? careerService.getAllCareers() : http.get(endpoint);
        request.then(({ data }) => {
            if (active) setResult({ endpoint, rows: Array.isArray(data) ? data : [data], error: "", loading: false });
        }).catch(error => {
            if (active) setResult({ endpoint, rows: [], error: adminErrorMessage(error), loading: false });
        });
        return () => { active = false; };
    }, [endpoint, version]);

    return {
        rows: result.endpoint === endpoint ? result.rows : [],
        error: result.endpoint === endpoint ? result.error : "",
        loading: Boolean(endpoint) && (result.endpoint !== endpoint || result.loading),
        reload: () => setVersion(value => value + 1)
    };
}

export const matchesSearch = (row, search, fields) => fields.some(field =>
    String(row[field] ?? "").toLocaleLowerCase("es-CL").includes(search.trim().toLocaleLowerCase("es-CL"))
);

export const matchesFilter = (value, filter) => filter === "" || (value != null && String(value) === String(filter));

export function filterValues(rows, field, selected = "") {
    const values = [...new Set(rows.map(row => row[field]).filter(value => value != null && value !== ""))];
    // Keep a previously selected real value visible if an edit removes its last match.
    if (selected !== "" && !values.some(value => String(value) === String(selected))) values.push(selected);
    return values.sort((first, second) => String(first).localeCompare(String(second), "es-CL", { numeric: true }));
}

// A missing catalogue must not turn an active relational filter into a false empty result.
export function filteredRecords(records, rows, dependencies = []) {
    const sources = [records, ...dependencies];
    const failed = sources.find(source => source.error);
    return {
        ...records, rows,
        loading: sources.some(source => source.loading),
        error: failed?.error || "",
        reload: failed ? failed.reload : records.reload
    };
}

// PUT still receives its complete DTO; unselected fields come from a fresh GET.
export async function saveAdminFields(endpoint, id, values, dtoFields, editedFields) {
    const { data: current } = await http.get(`${endpoint}/${id}`);
    const data = Object.fromEntries(dtoFields.map(field => [
        field, editedFields.includes(field) ? values[field] : current[field]
    ]));
    return http.put(`${endpoint}/${id}`, data);
}
