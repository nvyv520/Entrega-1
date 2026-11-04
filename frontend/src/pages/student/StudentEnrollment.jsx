import { useCallback, useEffect, useRef, useState } from "react";
import { useBeforeUnload, useBlocker } from "react-router-dom";

import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Snackbar,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography
} from "@mui/material";

import enrollmentService from "../../services/enrollment.service";
import { firstOpenAcademicPeriod, studentErrorMessage } from "../../services/studentAcademic";
import { studentScheduleSx } from "../../styles/studentContent";

const days = [
    { code: "L", label: "Lunes" },
    { code: "M", label: "Martes" },
    { code: "W", label: "Miércoles" },
    { code: "J", label: "Jueves" },
    { code: "V", label: "Viernes" }
];

const blocks = [1, 2, 3, 4, 5, 6];

// manage the student selection using enrollment.service.js endpoints
function StudentEnrollment() {

    const [periods, setPeriods] = useState([]);
    const [selectedPeriodId, setSelectedPeriodId] = useState("");

    const [subjects, setSubjects] = useState([]);
    const [sections, setSections] = useState([]);
    const [enrollments, setEnrollments] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [enrollingSectionId, setEnrollingSectionId] = useState(null);
    const [success, setSuccess] = useState("");
    const [selectionNoticeDismissed, setSelectionNoticeDismissed] = useState(false);

    const [verifying, setVerifying] = useState(false);
    const [verified, setVerified] = useState(null);
    const [dataReady, setDataReady] = useState(false);
    const [retry, setRetry] = useState(0);
    const operationRef = useRef(false);
    const [cancellingId, setCancellingId] = useState(null);
    const [discarding, setDiscarding] = useState(false);
    const [discardError, setDiscardError] = useState("");
    const busy = verifying || enrollingSectionId !== null || discarding;

    useEffect(() => {
        let active = true;
        const loadInitialData = async () => {
            setLoading(true);
            setDataReady(false);
            setError("");
            setSuccess("");
            setSelectionNoticeDismissed(false);
            setVerified(null);
            setSelectedPeriodId("");
            setSections([]);
            setEnrollments([]);
            try {
                const [studentResponse, periodsResponse] = await Promise.all([
                    enrollmentService.getCurrentStudent(),
                    enrollmentService.getOpenAcademicPeriods()
                ]);
                const period = firstOpenAcademicPeriod(periodsResponse.data);
                const [subjectsResponse, enrollmentsResponse, sectionsResponse, statusResponse] = await Promise.all([
                    enrollmentService.getSubjectsByStudyPlan(studentResponse.data.studyPlanId),
                    enrollmentService.getCurrentEnrollments(true),
                    period ? enrollmentService.getEligibleSections(period.id) : Promise.resolve({ data: [] }),
                    period ? enrollmentService.getVerificationStatus(period.id) : Promise.resolve({ data: false })
                ]);
                if (typeof statusResponse.data !== "boolean") throw new Error("Respuesta de verificación no válida");
                if (active) {
                    setSubjects(subjectsResponse.data);
                    setEnrollments(enrollmentsResponse.data);
                    setPeriods(periodsResponse.data);
                    setSelectedPeriodId(period?.id || "");
                    setSections(sectionsResponse.data);
                    setVerified(statusResponse.data);
                    setDataReady(true);
                }
            } catch (err) {
                if (active) setError(studentErrorMessage(err,
                    "No se pudo cargar la información de inscripción o consultar su verificación. Reintenta la carga."));
            } finally {
                if (active) setLoading(false);
            }
        };
        loadInitialData();
        return () => { active = false; };
    }, [retry]);

    const refreshSelection = async () => {
        // Reloads reservations, available sections and persistent confirmation together
        const [enrollmentsResponse, sectionsResponse, statusResponse] = await Promise.all([
            enrollmentService.getCurrentEnrollments(true),
            enrollmentService.getEligibleSections(selectedPeriodId),
            enrollmentService.getVerificationStatus(selectedPeriodId)
        ]);
        if (typeof statusResponse.data !== "boolean") throw new Error("Respuesta de verificación no válida");
        setEnrollments(enrollmentsResponse.data);
        setSections(sectionsResponse.data);
        setVerified(statusResponse.data);
        setSelectionNoticeDismissed(false);
    };

    const changeSelection = async (id, cancel) => {
        if (operationRef.current || !dataReady || !selectedPeriodId) return;
        operationRef.current = true;
        setEnrollingSectionId(id);
        setCancellingId(cancel ? id : null);
        setError("");
        setSuccess("");
        let saved = false;
        try {
            if (cancel) await enrollmentService.cancelEnrollment(id);
            else await enrollmentService.createEnrollment(id);
            saved = true;
            setVerified(null);
            await refreshSelection();
            setSuccess(cancel ? "Inscripción cancelada correctamente. Cupo liberado."
                : "Asignatura inscrita correctamente. Cupo reservado.");
        } catch (err) {
            if (saved) {
                setDataReady(false);
                setError("El cambio se guardó, pero no se pudo actualizar la información. Recarga los datos antes de continuar.");
            } else {
                if (!err.response || err.response.status >= 500) {
                    setDataReady(false);
                    setVerified(null);
                }
                setError(studentErrorMessage(err, cancel ? "No se pudo cancelar la inscripción. Inténtalo nuevamente."
                    : "No se pudo inscribir la asignatura. Inténtalo nuevamente."));
            }
        } finally {
            operationRef.current = false;
            setEnrollingSectionId(null);
            setCancellingId(null);
        }
    };
    const handleEnrollment = sectionId => changeSelection(sectionId, false);
    const handleCancelEnrollment = enrollmentId => changeSelection(enrollmentId, true);

    const handleVerifySelection = async () => {
        if (operationRef.current || !dataReady || !selectedPeriodId) return;
        operationRef.current = true;
        setVerifying(true);
        setError("");
        setSuccess("");
        let saved = false;
        try {
            await enrollmentService.verifySelection(selectedPeriodId);
            saved = true;
            setVerified(null);
            await refreshSelection();
            setSuccess("Verificación realizada. Se actualizó el estado de tu selección.");
        } catch (err) {
            if (saved) {
                setDataReady(false);
                setVerified(null);
                setError("La verificación se guardó, pero no se pudo actualizar la información. Recarga los datos antes de continuar.");
            } else {
                if (!err.response || err.response.status >= 500) {
                    setDataReady(false);
                    setVerified(null);
                }
                setError(studentErrorMessage(err, "No se pudo verificar tu selección. Reintenta la operación; no fue posible confirmar el resultado."));
            }
        } finally {
            operationRef.current = false;
            setVerifying(false);
        }
    };

    // The backend includes academically eligible sections and existing active enrollments.
    const offeredSections = sections;

    // Active enrollments for the selected academic period
    const activeEnrollments = enrollments.filter(enrollment =>
        enrollment.status === "ACTIVO" &&
        Number(enrollment.academicPeriodId) === Number(selectedPeriodId)
    );

    const enrolledCourses = activeEnrollments.map(enrollment => {

        // Builds the provisional schedule from the selected sections
        const section = sections.find(
            item => item.id === enrollment.sectionId
        );

        const subject = subjects.find(
            item => item.id === section?.subjectId
        );

        return {
            sectionId: enrollment.sectionId,
            code: subject?.code || "",
            name: subject?.name || "",
            blocks: section?.blocks || []
        };

    });

    const selectedPeriod = periods.find(
        period => period.id === selectedPeriodId
    );

    const hasProvisionalSelection = activeEnrollments.length > 0 && verified !== true;
    const blocker = useBlocker(({ currentLocation, nextLocation }) =>
        currentLocation.pathname !== nextLocation.pathname &&
        (hasProvisionalSelection || operationRef.current)
    );

    // Browser exits can warn about reservations but cannot guarantee cancellation.
    useBeforeUnload(useCallback(event => {
        if (hasProvisionalSelection || operationRef.current) {
            event.preventDefault();
            event.returnValue = "";
        }
    }, [hasProvisionalSelection]));

    useEffect(() => {
        if (blocker.state !== "blocked") setDiscardError("");
        // A completed confirmation or an empty selection no longer requires discarding.
        if (blocker.state === "blocked" && !busy && dataReady &&
            !hasProvisionalSelection && !operationRef.current) {
            blocker.proceed();
        }
    }, [blocker, busy, dataReady, hasProvisionalSelection]);

    const discardSelection = async () => {
        if (operationRef.current || !dataReady || verified !== false || blocker.state !== "blocked") return;
        operationRef.current = true;
        setDiscarding(true);
        setDiscardError("");
        try {
            // The backend cancels all reservations or rolls the whole operation back.
            await enrollmentService.discardSelection(selectedPeriodId);
            setEnrollments(current => current.filter(enrollment =>
                Number(enrollment.academicPeriodId) !== Number(selectedPeriodId)
            ));
            setVerified(false);
            blocker.proceed();
        } catch (err) {
            setDiscardError(studentErrorMessage(err,
                "No se pudo confirmar el descarte. Permaneces en esta página; revisa tu selección antes de reintentar."));
            if (!err.response || err.response.status >= 500) {
                setDataReady(false);
                setVerified(null);
            }
        } finally {
            operationRef.current = false;
            setDiscarding(false);
        }
    };

    if (loading) {
        return (
            <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Box>

            <Dialog open={blocker.state === "blocked"} onClose={busy ? undefined : () => blocker.reset()}
                aria-labelledby="selection-warning-title" aria-describedby="selection-warning-description">
                <DialogTitle id="selection-warning-title">Selección sin confirmar</DialogTitle>
                <DialogContent>
                    <DialogContentText id="selection-warning-description">
                        Tienes asignaturas sin confirmar. Si sales, puedes perder tu selección provisional.
                    </DialogContentText>
                    {busy && <Alert severity="info" sx={{ mt: 2 }}>Espera a que termine la operación en curso.</Alert>}
                    {!dataReady && <Alert severity="warning" sx={{ mt: 2 }}>Continúa inscribiendo y recarga los datos antes de descartar la selección.</Alert>}
                    {discardError && <Alert severity="error" sx={{ mt: 2 }}>{discardError}</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button disabled={busy} onClick={() => blocker.reset()}>Seguir inscribiendo</Button>
                    <Button color="error" disabled={busy || !dataReady || verified !== false} onClick={discardSelection}>
                        Descartar selección
                    </Button>
                </DialogActions>
            </Dialog>

            <Typography
                variant="h5"
                sx={{ fontWeight: 600, mb: 2 }}
            >
                Inscripción de asignaturas
            </Typography>

            {error && (
                <Alert severity="error" onClose={() => setError("")} sx={{ mb: 2 }}>
                    {error}
                </Alert>
            )}

            {!dataReady && (
                <Button variant="outlined" onClick={() => setRetry(value => value + 1)} disabled={busy} sx={{ mb: 2 }}>
                    Recargar datos
                </Button>
            )}

            <Snackbar
                open={Boolean(success)}
                autoHideDuration={6000}
                onClose={(_event, reason) => {
                    if (reason !== "clickaway") setSuccess("");
                }}
                anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
            >
                <Alert severity="success" onClose={() => setSuccess("")} sx={{ fontSize: "14px" }}>
                    {success}
                </Alert>
            </Snackbar>


            {/* Current academic period */}
            {selectedPeriod ? (
                <Box
                    sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 2,
                        mb: 3
                    }}
                >
                    <Typography variant="body2">
                        Período académico:
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{ fontWeight: 600 }}
                    >
                        {selectedPeriod.code}
                    </Typography>
                </Box>
            ) : dataReady ? (
                <Alert severity="info" sx={{ mb: 3 }}>
                    No hay períodos académicos abiertos para inscripción.
                </Alert>
            ) : null}


            {selectedPeriod && dataReady && !busy && verified !== null && !selectionNoticeDismissed && (
                <Alert
                    severity={verified ? "success" : "info"}
                    onClose={() => setSelectionNoticeDismissed(true)}
                    sx={{ mb: 3 }}
                >
                    {activeEnrollments.length === 0
                        ? "Comienza seleccionando una asignatura de las secciones ofertadas."
                        : verified
                            ? "Inscripción confirmada. Puedes seguir seleccionando o cancelando mientras el período esté abierto; después deberás verificar nuevamente."
                            : "Selección provisional, sin inscripción oficial. Tus cupos ya están reservados. Verifica al completar entre 3 y 6 asignaturas, salvo que te falten menos de 3 para terminar el plan."}
                </Alert>
            )}

            {/* Main content */}
            <Box
                sx={{
                    display: "grid",
                    gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
                    gap: 3,
                    alignItems: "start"
                }}
            >

                {/* Left column */}
                <Box>
                    <Typography
                        variant="h6"
                        sx={{ fontWeight: 600, mb: 2 }}
                    >
                        Secciones ofertadas
                    </Typography>

                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow sx={{ bgcolor: "var(--curriculum-blue)" }}>
                                    {[
                                        "Código",
                                        "Asignatura",
                                        "Sección",
                                        "Docente",
                                        "Cupos",
                                        "Bloques",
                                        "Acción"
                                    ].map(column => (
                                        <TableCell
                                            key={column}
                                            sx={{
                                                color: "#FFFFFF",
                                                fontWeight: 600
                                            }}
                                        >
                                            {column}
                                        </TableCell>
                                    ))}
                                </TableRow>
                            </TableHead>

                            <TableBody>
                                {offeredSections.map(section => {

                                    const subject = subjects.find(
                                        item => item.id === section.subjectId
                                    );

                                    const remaining =
                                        Math.max(0, section.capacity - section.enrolledStudents);

                                    const currentEnrollment = activeEnrollments.find(
                                        enrollment => enrollment.sectionId === section.id
                                    );

                                    return (
                                        <TableRow key={section.id}>
                                            <TableCell>
                                                {subject?.code}
                                            </TableCell>

                                            <TableCell>
                                                {subject?.name}
                                            </TableCell>

                                            <TableCell>
                                                {section.code}
                                            </TableCell>

                                            <TableCell>
                                                {section.teacherName?.trim() || "Sin asignar"}
                                            </TableCell>

                                            <TableCell>
                                                {remaining}
                                            </TableCell>

                                            <TableCell>
                                                {[...section.blocks].sort().join(", ")}
                                            </TableCell>

                                            
                                            <TableCell>
                                                {currentEnrollment ? (
                                                    <Button
                                                        variant="outlined"
                                                        size="small"
                                                        onClick={() =>
                                                            handleCancelEnrollment(currentEnrollment.id)
                                                        }
                                                        disabled={
                                                            busy || !dataReady
                                                        }
                                                        sx={{
                                                            color: "var(--curriculum-blue)",
                                                            borderColor: "var(--curriculum-blue)",
                                                            textTransform: "none",
                                                        }}
                                                    >
                                                        {cancellingId === currentEnrollment.id
                                                            ? "Cancelando..."
                                                            : "Cancelar"}
                                                    </Button>
                                                ) : (
                                                    <Button
                                                        variant="contained"
                                                        size="small"
                                                        onClick={() => handleEnrollment(section.id)}
                                                        disabled={
                                                            remaining <= 0 ||
                                                            busy || !dataReady
                                                        }
                                                        sx={{
                                                            backgroundColor: "var(--primary-color)",
                                                            textTransform: "none",
                                                            "&:hover": {
                                                                backgroundColor: "var(--curriculum-blue)"
                                                            }
                                                        }}
                                                    >
                                                        {cancellingId === null && enrollingSectionId === section.id
                                                            ? "Seleccionando..."
                                                            : "Seleccionar"}
                                                    </Button>
                                                )}
                                            </TableCell>

                                        </TableRow>
                                    );
                                })}

                                {offeredSections.length === 0 && (
                                    <TableRow>
                                        <TableCell colSpan={7} align="center">
                                            No hay secciones académicamente habilitadas para ti en este período.
                                        </TableCell>
                                    </TableRow>
                                )}
                            </TableBody>
                        </Table>
                    </TableContainer>

                    {selectedPeriod && (
                        <Typography
                            variant="caption"
                            sx={{
                                display: "block",
                                mt: 1,
                                color: "var(--secondary-text-color)"
                            }}
                        >
                            Seleccionar reserva un cupo provisional; cancelar lo libera.
                            Verificar confirma la inscripción sin descontar otro cupo y, si falla, conserva tu selección provisional.
                        </Typography>
                    )}
                </Box>

                {/* Right column */}
                <Box>
                    <Typography
                        variant="h6"
                        sx={{ fontWeight: 600, mb: 2 }}
                    >
                        Horario provisional de selección
                    </Typography>

                    <TableContainer sx={studentScheduleSx}>
                        <Table size="small" sx={{ tableLayout: "fixed" }}>
                            <TableHead>
                                <TableRow>
                                    <TableCell
                                        sx={{
                                            bgcolor: "var(--curriculum-blue)",
                                            color: "#FFFFFF",
                                            fontWeight: 600,
                                            width: 80
                                        }}
                                    >
                                        Bloque
                                    </TableCell>

                                    {days.map(day => (
                                        <TableCell
                                            key={day.code}
                                            align="center"
                                            sx={{
                                                bgcolor: "var(--curriculum-blue)",
                                                color: "#FFFFFF",
                                                fontWeight: 600
                                            }}
                                        >
                                            {day.label}
                                        </TableCell>
                                    ))}
                                </TableRow>
                            </TableHead>

                            <TableBody>
                                {blocks.map(block => (
                                    <TableRow key={block}>
                                        <TableCell
                                            sx={{
                                                fontWeight: 600,
                                                textAlign: "center"
                                            }}
                                        >
                                            Bloque {block}
                                        </TableCell>

                                        {days.map(day => {

                                            const blockCode = `${day.code}${block}`;

                                            const course = enrolledCourses.find(
                                                item =>
                                                    item.blocks.includes(blockCode)
                                            );

                                            return (
                                                <TableCell
                                                    key={blockCode}
                                                    align="center"
                                                    sx={{
                                                        height: 72,
                                                        p: 1,
                                                        bgcolor: course
                                                            ? "var(--in-progress-color)"
                                                            : "#FFFFFF"
                                                    }}
                                                >
                                                    {course && (
                                                        <Typography
                                                            variant="caption"
                                                            fontWeight={600}
                                                        >
                                                            {course.code}
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                            );

                                        })}
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>

                    <Typography
                        variant="body2"
                        sx={{
                            mt: 2,
                            color: "var(--secondary-text-color)"
                        }}
                    >
                        Asignaturas seleccionadas: {activeEnrollments.length}
                    </Typography>

                    <Button
                        variant="contained"
                        onClick={handleVerifySelection}
                        disabled={
                            activeEnrollments.length === 0 ||
                            busy || !dataReady || !selectedPeriodId
                        }
                        sx={{
                            mt: 2,
                            backgroundColor: "var(--primary-color)",
                            textTransform: "none",
                            "&:hover": {
                                backgroundColor: "var(--curriculum-blue)"
                            }
                        }}
                    >
                        {verifying
                            ? "Verificando..."
                            : "Verificar selección"}
                    </Button>


                </Box>
            </Box>
        </Box>
    );
}

export default StudentEnrollment;
