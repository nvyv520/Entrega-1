import { useEffect, useState } from "react";

import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Typography
} from "@mui/material";

import subjectService from "../../services/subject.service";
import { getCurrentSelection, studentErrorMessage } from "../../services/studentAcademic";

// Eligibility depends on academic records, independently of the displayed status/color.
function hasApprovedPrerequisites(subject, history) {
    return (subject.prerequisiteIds || []).every(prerequisiteId => history.some(record =>
        Number(record.subjectId) === Number(prerequisiteId) && record.result === "APROBADO"
    ));
}

// determine the academic status of a subject
function getSubjectStatus(subject, history, currentSubjectIds) {
    const subjectHistory = history.filter(
        record => Number(record.subjectId) === Number(subject.id)
    );

    // check if the subject was approved
    const approved = subjectHistory.some(
        record => record.result === "APROBADO"
    );

    if (approved) {
        return "approved";
    }

    // Check if the subject is currently enrolled
    if (currentSubjectIds.has(Number(subject.id))) {
        return "in-progress";
    }

    // check if the subject was failed
    const failed = subjectHistory.some(
        record => record.result === "REPROBADO"
    );

    if (failed) {
        return "failed";
    }

    // check prerequisites
    const prerequisitesApproved = hasApprovedPrerequisites(subject, history);

    if (prerequisitesApproved) {
        return "eligible";
    }

    return "blocked";
}

const statusStyles = {
    approved: {
        label: "Aprobada",
        color: "#CDEBCF"
    },
    "in-progress": {
        label: "Cursando",
        color: "var(--in-progress-color)"
    },
    eligible: {
        label: "Con derecho a inscripción",
        color: "var(--eligible-color)"
    },
    failed: {
        label: "Reprobada",
        color: "var(--failed-color)"
    },
    blocked: {
        label: "Bloqueada por prerrequisitos",
        color: "var(--blocked-color)"
    }
};

function StudentStudyPlan() {


    /* Stores the information */
    const [subjects, setSubjects] = useState([]);
    const [history, setHistory] = useState([]);
    const [currentSubjectIds, setCurrentSubjectIds] = useState(new Set());
    const [careerName, setCareerName] = useState("");
    const [studyPlanCode, setStudyPlanCode] = useState("");

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [retry, setRetry] = useState(0);
    const [selectionMessage, setSelectionMessage] = useState("");
    const [selectionError, setSelectionError] = useState(false);

    /* Loads the data */
    useEffect(() => {

        let active = true;

        const loadStudyPlan = async () => {
            setLoading(true);
            setError("");

            try {
                const studentResponse =
                    await subjectService.getCurrentStudent();

                const studyPlanId = studentResponse.data.studyPlanId;
                
                const careerId = studentResponse.data.careerId;

                const [studyPlanResponse, careerResponse] = await Promise.all([
                    subjectService.getStudyPlanById(studyPlanId),
                    subjectService.getCareerById(careerId)
                ]);

                if (active) {
                    setStudyPlanCode(studyPlanResponse.data.identifier);
                    setCareerName(careerResponse.data.name);
                }

                const [subjectsResponse, historyResponse, selectionResult] = await Promise.all([
                    subjectService.getSubjectsByStudyPlan(studyPlanId),
                    subjectService.getCurrentStudentAcademicHistory(),
                    getCurrentSelection().then(data => ({ data })).catch(error => ({ error }))
                ]);

                const inProgressIds = new Set();
                let message = "";
                if (selectionResult.error) {
                    message = studentErrorMessage(selectionResult.error,
                        "No se pudo consultar la selección actual. Se conserva el historial; el estado Cursando no está disponible. Vuelve a ingresar para reintentar.");
                } else {
                    const selection = selectionResult.data;
                    if (!selection.period) {
                        message = "No hay un período académico abierto. Se muestran los estados del historial y los prerrequisitos.";
                    }
                    const sectionIds = new Set(selection.enrollments.map(enrollment => Number(enrollment.sectionId)));
                    selection.sections.filter(section => sectionIds.has(Number(section.id)))
                        .forEach(section => inProgressIds.add(Number(section.subjectId)));
                }


                if (active) {
                    setSelectionError(Boolean(selectionResult.error));
                    setSelectionMessage(message);
                    setSubjects(subjectsResponse.data);
                    setHistory(historyResponse.data);
                    setCurrentSubjectIds(inProgressIds);
                }

            } catch (error) {

                console.error("Error loading study plan:", error);

                if (active) {
                    setError(studentErrorMessage(error, "No se pudo cargar el plan de estudio. Vuelve a ingresar a esta vista para reintentar."));
                }

            } finally {

                if (active) {
                    setLoading(false);
                }
            }
        };

        loadStudyPlan();

        return () => {
            active = false;
        };

    }, [retry]);

    // group subjects by semester
    const semesters = [...new Set(
        subjects.map(subject => subject.semester)
    )].sort((a, b) => a - b);

    if (loading) {
        return (
            <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
                <CircularProgress />
            </Box>
        );
    }

    if (error) {
        return <Box>
            <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
            <Button variant="outlined" onClick={() => setRetry(value => value + 1)}>Reintentar</Button>
        </Box>;
    }

    return (
        <Box>
            <Box sx={{ mb: 3 }}>
            <Typography
                    variant="h5"
                    sx={{ fontWeight: 600 }}
                >
                    Plan de estudio
                </Typography>

                <Typography
                    variant="body2"
                    sx={{
                        color: "var(--secondary-text-color)",
                        mt: 0.5
                    }}
                >
                    {careerName}
                    {studyPlanCode && ` — Plan ${studyPlanCode}`}
                </Typography>
            </Box>


            {selectionMessage && <Alert severity={selectionError ? "warning" : "info"} sx={{ mb: 3 }}>
                {selectionMessage}
                {selectionError && <Button onClick={() => setRetry(value => value + 1)}>Reintentar</Button>}
            </Alert>}

            {/* Study plan organized by semester */}
            <Box
                sx={{
                    display: "grid",
                    gridTemplateColumns:
                        `repeat(${semesters.length || 1}, minmax(190px, 1fr))`,
                    gap: 2,
                    overflowX: "auto"
                }}
            >
                {semesters.map(semester => (
                    <Box key={semester}>
                        <Box
                            sx={{
                                bgcolor: "var(--curriculum-blue)",
                                color: "#FFFFFF",
                                textAlign: "center",
                                borderRadius: "10px",
                                py: 1,
                                mb: 2
                            }}
                        >
                            <Typography fontWeight={600}>
                                Semestre {semester}
                            </Typography>
                        </Box>

                        {subjects
                            .filter(subject => subject.semester === semester)
                            .sort((a, b) => a.code.localeCompare(b.code))
                            .map(subject => {

                                const status = getSubjectStatus(
                                    subject,
                                    history,
                                    currentSubjectIds
                                );

                                return (
                                    <Box
                                        key={subject.id}
                                        title={statusStyles[status].label}
                                        sx={{
                                            bgcolor: statusStyles[status].color,
                                            color: status === "approved" ? "#285D39" : "inherit",
                                            border: "1px solid var(--border-color)",
                                            borderRadius: "10px",
                                            p: 1.5,
                                            mb: 1.5,
                                            minHeight: 90
                                        }}
                                    >
                                        <Typography
                                            variant="caption"
                                            fontWeight={700}
                                        >
                                            {subject.code}
                                        </Typography>

                                        <Typography
                                            variant="body2"
                                            sx={{
                                                mt: 0.5,
                                                fontWeight: 500
                                            }}
                                        >
                                            {subject.name}
                                        </Typography>
                                        <Typography variant="caption" display="block">{statusStyles[status].label}</Typography>
                                        {status === "failed" && <Typography variant="caption" display="block">
                                            {hasApprovedPrerequisites(subject, history)
                                                ? "Prerrequisitos aprobados"
                                                : "Prerrequisitos pendientes"}
                                        </Typography>}
                                    </Box>
                                );
                            })}
                    </Box>
                ))}
            </Box>


            
            {/* Academic status legend */}
            <Box
                sx={{
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    flexWrap: "wrap",
                    gap: 3,
                    mt: 4,
                    mb: 2
                }}
            >
                {Object.entries(statusStyles).map(([status, style]) => (
                    <Box
                        key={status}
                        sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1
                        }}
                    >
                        <Box
                            sx={{
                                width: 14,
                                height: 14,
                                bgcolor: style.color,
                                border: "1px solid var(--border-color)"
                            }}
                        />

                        <Typography variant="body2">
                            {style.label}
                        </Typography>
                    </Box>
                ))}
            </Box>


            {subjects.length === 0 && (
                <Alert severity="info" sx={{ mt: 2 }}>
                    No hay asignaturas registradas para este plan.
                </Alert>
            )}
        </Box>
    );
}

export default StudentStudyPlan;
