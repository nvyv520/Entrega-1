import { createContext, useContext, useEffect, useState } from "react";
import http from "../http-common";

export const TeacherContext = createContext(null);
export const useTeacherCatalog = () => useContext(TeacherContext);

export function teacherErrorMessage(error) {
    const data = error.response?.data;
    const reason = typeof data === "string" ? data : data?.detail || data?.message;
    if (typeof reason === "string" && reason.trim()) return reason;
    if (!error.isAxiosError && error.message) return error.message;
    return {
        400: "Revisa los datos. La nota debe estar entre 1,0 y 7,0 y tener como máximo un decimal.",
        401: "La sesión expiró. Vuelve a iniciar sesión.",
        403: "No tienes permiso para consultar o modificar esta sección.",
        404: "No se encontró la información. Comprueba que tu cuenta esté vinculada a un docente.",
        409: "La operación no está permitida. El período puede estar cerrado, la inscripción inactiva o la nota ya registrada."
    }[error.response?.status] || "No se pudo completar la solicitud. Intenta nuevamente.";
}

export function useTeacherRecords(endpoint) {
    const [result, setResult] = useState({ endpoint: null, rows: [], loading: true, error: "" });
    const [version, setVersion] = useState(0);

    useEffect(() => {
        let active = true;
        if (!endpoint) return;
        setResult({ endpoint, rows: [], loading: true, error: "" });
        http.get(endpoint).then(({ data }) => {
            if (active) setResult({ endpoint, rows: Array.isArray(data) ? data : [data], loading: false, error: "" });
        }).catch(error => {
            if (active) setResult({ endpoint, rows: [], loading: false, error: teacherErrorMessage(error) });
        });
        return () => { active = false; };
    }, [endpoint, version]);

    return {
        rows: endpoint && result.endpoint === endpoint ? result.rows : [],
        loading: Boolean(endpoint) && (result.endpoint !== endpoint || result.loading),
        error: endpoint && result.endpoint === endpoint ? result.error : "",
        reload: () => setVersion(value => value + 1)
    };
}

export const teacherMatchesSearch = (row, search, fields) => fields.some(field =>
    String(row[field] ?? "").toLocaleLowerCase("es-CL").includes(search.trim().toLocaleLowerCase("es-CL"))
);

export function parseFinalGrade(value) {
    if (/\s/u.test(value) || !/^[1-7](?:[.,][0-9])?$/.test(value)) {
        throw new Error("Ingresa una nota entre 1,0 y 7,0 con un máximo de un decimal.");
    }
    const grade = Number(value.replace(",", "."));
    if (grade < 1 || grade > 7) throw new Error("La nota debe estar entre 1,0 y 7,0.");
    return grade;
}

export const formatFinalGrade = value => value == null ? "Sin nota" : Number(value).toFixed(1).replace(".", ",");
