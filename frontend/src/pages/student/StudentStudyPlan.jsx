import { useEffect, useState } from "react";

import {
    Alert,
    Box,
    CircularProgress,
    Typography
} from "@mui/material";

import subjectService from "../../services/subject.service";

// determine the academic status of a subject
function getSubjectStatus(subject, history, enrollments) {

    const subjectHistory = history.filter(
        record => record.subjectId === subject.id
    );

    // check if the subject was approved
    const approved = subjectHistory.some(
        record => record.result === "APROBADO"
    );

    if (approved) {
        return "approved";
    }

    // check if the student is currently enrolled
    const currentEnrollment = subjectHistory.some(record =>
        record.enrollmentStatus === "ACTIVO" &&
        record.result === null &&
        enrollments.some(enrollment =>
            enrollment.id === record.enrollmentId &&
            enrollment.status === "ACTIVO"
        )
    );

    if (currentEnrollment) {
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
    const prerequisitesApproved = (subject.prerequisiteIds || []).every(
        prerequisiteId => history.some(record =>
            record.subjectId === prerequisiteId &&
            record.result === "APROBADO"
        )
    );

    if (prerequisitesApproved) {
        return "eligible";
    }

    return "blocked";
}

const statusStyles = {
    approved: {
        label: "Aprobada",
        color: "var(--approved-color)"
    },
    "in-progress": {
        label: "Cursando",
        color: "var(--in-progress-color)"
    },
    eligible: {
        label: "Con derecho",
        color: "var(--eligible-color)"
    },
    failed: {
        label: "Reprobada",
        color: "var(--failed-color)"
    },
    blocked: {
        label: "Bloqueada",
        color: "var(--blocked-color)"
    }
};

function StudentStudyPlan() {


    /* guardar informacion */
    const [subjects, setSubjects] = useState([]);
    const [history, setHistory] = useState([]);
    const [enrollments, setEnrollments] = useState([]);
    const [careerName, setCareerName] = useState("");
    const [studyPlanCode, setStudyPlanCode] = useState("");

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    /* cargar datos */
    useEffect(() => {

        let active = true;

        const loadStudyPlan = async () => {

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

                const [
                    subjectsResponse,
                    historyResponse,
                    enrollmentsResponse
                ] = await Promise.all([
                    subjectService.getSubjectsByStudyPlan(studyPlanId),
                    subjectService.getCurrentStudentAcademicHistory(),
                    subjectService.getCurrentStudentEnrollments()
                ]);

                if (active) {
                    setSubjects(subjectsResponse.data);
                    setHistory(historyResponse.data);
                    setEnrollments(enrollmentsResponse.data);
                }

            } catch (error) {

                console.error("Error loading study plan:", error);

                if (active) {
                    setError("No se pudo cargar el plan de estudio");
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

    }, []);

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
        return <Alert severity="error">{error}</Alert>;
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
                                    enrollments
                                );

                                return (
                                    <Box
                                        key={subject.id}
                                        sx={{
                                            bgcolor: statusStyles[status].color,
                                            border: "1px solid var(--border-color)",
                                            borderRadius: "3px",
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
