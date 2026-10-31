import enrollmentService from "./enrollment.service";

// All student views use the same definition of the current academic period.
export const latestAcademicPeriod = periods => [...periods].sort(
    (a, b) => Number(b.year) - Number(a.year) || Number(b.semester) - Number(a.semester)
)[0] || null;

export async function getCurrentSelection() {
    const periodsResponse = await enrollmentService.getOpenAcademicPeriods();
    const period = latestAcademicPeriod(periodsResponse.data);
    if (!period) return { period: null, verified: false, enrollments: [], sections: [] };

    // A failed status request must reject, never silently become an unverified selection.
    const verificationResponse = await enrollmentService.getVerificationStatus(period.id);
    if (typeof verificationResponse.data !== "boolean") {
        throw new Error("Respuesta de verificación no válida");
    }
    const verified = verificationResponse.data;
    if (!verified) return { period, verified, enrollments: [], sections: [] };

    const [enrollmentsResponse, sectionsResponse] = await Promise.all([
        enrollmentService.getCurrentEnrollments(),
        enrollmentService.getSectionsByAcademicPeriod(period.id)
    ]);
    return {
        period,
        verified,
        enrollments: enrollmentsResponse.data.filter(enrollment =>
            enrollment.status === "ACTIVO" && Number(enrollment.academicPeriodId) === Number(period.id)
        ),
        sections: sectionsResponse.data
    };
}

export function studentErrorMessage(error, fallback) {
    const status = error.response?.status;
    if (status === 401) return "Tu sesión expiró. Vuelve a iniciar sesión.";
    if (status === 403) return "No tienes permiso para consultar o modificar esta información. Solicita revisar los permisos STUDENT de tu cuenta.";
    if (status === 404) return "No se encontró la información solicitada. Solicita revisar la vinculación de tu cuenta con SIGA.";
    if (status === 400 || status === 409) {
        const reason = error.response?.data?.detail || error.response?.data?.message;
        if (typeof reason === "string" && reason.trim()) return reason;
    }
    return fallback;
}
