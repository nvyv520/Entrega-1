
import { useEffect, useState } from "react";

import {
    Alert,
    Box,
    Button,
    CircularProgress,
    MenuItem,
    Select,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography
} from "@mui/material";

import enrollmentService from "../../services/enrollment.service";

const days = [
    { code: "L", label: "Lunes" },
    { code: "M", label: "Martes" },
    { code: "W", label: "Miércoles" },
    { code: "J", label: "Jueves" },
    { code: "V", label: "Viernes" }
];

const blocks = [1, 2, 3, 4, 5, 6];

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

    // Load student information and academic periods
    useEffect(() => {

        let active = true;

        const loadInitialData = async () => {

            try {
                const [studentResponse, periodsResponse, enrollmentsResponse] =
                    await Promise.all([
                        enrollmentService.getCurrentStudent(),
                        enrollmentService.getOpenAcademicPeriods(),
                        enrollmentService.getCurrentEnrollments()
                    ]);

                const studyPlanId = studentResponse.data.studyPlanId;

                const subjectsResponse =
                    await enrollmentService.getSubjectsByStudyPlan(studyPlanId);

                if (active) {
                    setSubjects(subjectsResponse.data);
                    setEnrollments(enrollmentsResponse.data);
                    setPeriods(periodsResponse.data);

                    if (periodsResponse.data.length > 0) {
                        setSelectedPeriodId(periodsResponse.data[0].id);
                    }
                }

            } catch (err) {
                console.error("Error loading enrollment data:", err);

                if (active) {
                    setError("No se pudo cargar la información de inscripción");
                }

            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        };

        loadInitialData();

        return () => {
            active = false;
        };

    }, []);

    // Load sections for the selected academic period
    useEffect(() => {

        if (!selectedPeriodId) {
            setSections([]);
            return;
        }

        let active = true;

        const loadSections = async () => {

            try {
                const response =
                    await enrollmentService.getSectionsByAcademicPeriod(
                        selectedPeriodId
                    );

                if (active) {
                    setSections(response.data);
                    setError("");
                }

            } catch (err) {
                console.error("Error loading sections:", err);

                if (active) {
                    setError("No se pudieron cargar las secciones");
                }
            }
        };

        loadSections();

        return () => {
            active = false;
        };

    }, [selectedPeriodId]);


    const handleCancelEnrollment = async (enrollmentId) => {

        setEnrollingSectionId(enrollmentId);
        setError("");
        setSuccess("");

        try {

            // Cancel student enrollment
            await enrollmentService.cancelEnrollment(enrollmentId);

            // Refresh enrollments and sections
            const [enrollmentsResponse, sectionsResponse] =
                await Promise.all([
                    enrollmentService.getCurrentEnrollments(),
                    enrollmentService.getSectionsByAcademicPeriod(
                        selectedPeriodId
                    )
                ]);

            setEnrollments(enrollmentsResponse.data);
            setSections(sectionsResponse.data);

            setSuccess("Inscripción cancelada correctamente");

        } catch (err) {

            console.error("Error canceling enrollment:", err);

            const message =
                err.response?.data?.detail ||
                err.response?.data?.message ||
                "No se pudo cancelar la inscripción";

            setError(message);

        } finally {
            setEnrollingSectionId(null);
        }
    };



    // Sections offered for subjects in the student's study plan
    const offeredSections = sections.filter(section =>
        section.status === "ACTIVA" &&
        subjects.some(subject => subject.id === section.subjectId)
    );

    // Active enrollments for the selected academic period
    const activeEnrollments = enrollments.filter(enrollment =>
        enrollment.status === "ACTIVO" &&
        enrollment.academicPeriodId === selectedPeriodId
    );

    const enrolledCourses = activeEnrollments.map(enrollment => {

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

    if (loading) {
        return (
            <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Box>

            <Typography
                variant="h5"
                sx={{ fontWeight: 600, mb: 2 }}
            >
                Inscripción de asignaturas
            </Typography>

            {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                </Alert>
            )}

            {success && (
                <Alert severity="success" sx={{ mb: 2 }}>
                    {success}
                </Alert>
            )}

            {/* Academic period selector */}
            {periods.length > 0 ? (
                <Box sx={{ display: "flex", alignItems: "center", gap: 2, mb: 3 }}>
                    <Typography variant="body2">
                        Período académico:
                    </Typography>

                    <Select
                        size="small"
                        value={selectedPeriodId}
                        onChange={event =>
                            setSelectedPeriodId(event.target.value)
                        }
                        sx={{ minWidth: 160 }}
                    >
                        {periods.map(period => (
                            <MenuItem key={period.id} value={period.id}>
                                {period.code}
                            </MenuItem>
                        ))}
                    </Select>
                </Box>
            ) : (
                <Alert severity="info" sx={{ mb: 3 }}>
                    No hay períodos académicos abiertos para inscripción.
                </Alert>
            )}

            {/* Main content */}
            <Box
                sx={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
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

                    <TableContainer
                        sx={{
                            border: "1px solid var(--border-color)",
                            borderRadius: "3px"
                        }}
                    >
                        <Table size="small">
                            <TableHead>
                                <TableRow sx={{ bgcolor: "var(--curriculum-blue)" }}>
                                    {[
                                        "Código",
                                        "Asignatura",
                                        "Sección",
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
                                        section.capacity -
                                        section.enrolledStudents;

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
                                                {section.id}
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
                                                        disabled={enrollingSectionId !== null}
                                                        sx={{
                                                            color: "var(--curriculum-blue)",
                                                            borderColor: "var(--curriculum-blue)",
                                                            textTransform: "none",
                                                            borderRadius: "3px"
                                                        }}
                                                    >
                                                        {enrollingSectionId === currentEnrollment.id
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
                                                            enrollingSectionId !== null
                                                        }
                                                        sx={{
                                                            backgroundColor: "var(--primary-color)",
                                                            textTransform: "none",
                                                            borderRadius: "3px",
                                                            "&:hover": {
                                                                backgroundColor: "var(--curriculum-blue)"
                                                            }
                                                        }}
                                                    >
                                                        {enrollingSectionId === section.id
                                                            ? "Inscribiendo..."
                                                            : "Inscribir"}
                                                    </Button>
                                                )}
                                            </TableCell>

                                        </TableRow>
                                    );
                                })}

                                {offeredSections.length === 0 && (
                                    <TableRow>
                                        <TableCell colSpan={6} align="center">
                                            No hay secciones ofertadas para este plan.
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
                            Los cupos se muestran según los datos actuales del sistema.
                        </Typography>
                    )}
                </Box>

                {/* Right column */}
                <Box>
                    <Typography
                        variant="h6"
                        sx={{ fontWeight: 600, mb: 2 }}
                    >
                        Horario de inscripción
                    </Typography>

                    <TableContainer
                        sx={{
                            border: "1px solid var(--border-color)",
                            borderRadius: "3px"
                        }}
                    >
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
                                                            : "#FFFFFF",
                                                        border:
                                                            "1px solid var(--border-color)"
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
                        Asignaturas inscritas: {activeEnrollments.length}
                    </Typography>
                </Box>
            </Box>
        </Box>
    );
}

export default StudentEnrollment;
