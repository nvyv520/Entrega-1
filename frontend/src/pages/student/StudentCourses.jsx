
import { useEffect, useState } from "react";

import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography
} from "@mui/material";

import subjectService from "../../services/subject.service";
import { getCurrentSelection, studentErrorMessage } from "../../services/studentAcademic";
import { studentScheduleSx } from "../../styles/studentContent";



const days = [
    { code: "L", label: "Lunes" },
    { code: "M", label: "Martes" },
    { code: "W", label: "Miércoles" },
    { code: "J", label: "Jueves" },
    { code: "V", label: "Viernes" }
];

const blocks = [1, 2, 3, 4, 5, 6];

// get confirmed courses through studentAcademic.js and subject.service.js
function StudentCourses() {

    const [verified, setVerified] = useState(false);
    const [courses, setCourses] = useState([]);
    const [periodCode, setPeriodCode] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [retry, setRetry] = useState(0);

    useEffect(() => {

        let active = true;

        const loadCourses = async () => {
            setLoading(true);
            setError("");

            try {

                const selection = await getCurrentSelection();
                const { period: currentPeriod, verified: isVerified, enrollments, sections } = selection;
                let coursesData = [];
                // Builds the definitive schedule only from a confirmed selection
                if (isVerified && enrollments.length > 0) {
                    const studentResponse = await subjectService.getCurrentStudent();
                    const subjectsResponse = await subjectService.getSubjectsByStudyPlan(studentResponse.data.studyPlanId);
                    coursesData = enrollments.map(enrollment => {
                        const section = sections.find(item => Number(item.id) === Number(enrollment.sectionId));
                        const subject = subjectsResponse.data.find(item => Number(item.id) === Number(section?.subjectId));
                        if (!section || !subject) throw new Error("Información de curso incompleta");
                        return {
                            enrollmentId: enrollment.id,
                            sectionId: section.id,
                            sectionCode: section.code,
                            subjectCode: subject.code,
                            subjectName: subject.name,
                            teacherName: section.teacherName || "Docente no informado",
                            blocks: section.blocks || []
                        };
                    });
                }

                if (active) {
                    setCourses(coursesData);
                    setVerified(isVerified);
                    setPeriodCode(currentPeriod?.code || "");
                }

            } catch (error) {

                console.error("Error loading courses:", error);

                if (active) {
                    setError(studentErrorMessage(error, "No se pudieron cargar los cursos o consultar la verificación de tu selección. Vuelve a ingresar a esta vista para reintentar."));
                }

            } finally {

                if (active) {
                    setLoading(false);
                }
            }
        };

        loadCourses();

        return () => {
            active = false;
        };

    }, [retry]);

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

            <Typography
                variant="h5"
                sx={{ fontWeight: 600, mb: 3 }}
            >
                Mis cursos
            </Typography>


            <Typography variant="body2" sx={{ mb: 2 }}>
                {periodCode
                    ? `Período académico: ${periodCode}`
                    : "No hay un período académico abierto."}
            </Typography>


            {courses.length === 0 ? (
                
            (!periodCode || verified) && <Alert severity="info" sx={{ mb: 3 }}>
                No tienes cursos verificados en el período académico actual.
            </Alert>
            
            ) : (
                <>

                    {/* Enrolled courses table */}
                    <TableContainer
                        sx={{
                            mb: 4
                        }}
                    >
                        <Table size="small">
                            <TableHead>
                                <TableRow
                                    sx={{
                                        bgcolor: "var(--curriculum-blue)"
                                    }}
                                >
                                    {[
                                        "Código",
                                        "Asignatura",
                                        "Sección",
                                        "Docente",
                                        "Bloques"
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
                                {courses.map(course => (
                                    <TableRow key={course.enrollmentId}>
                                        <TableCell>
                                            {course.subjectCode}
                                        </TableCell>

                                        <TableCell>
                                            {course.subjectName}
                                        </TableCell>

                                        <TableCell>
                                            {course.sectionCode}
                                        </TableCell>

                                        <TableCell>
                                            {course.teacherName}
                                        </TableCell>

                                        <TableCell>
                                            {course.blocks.join(", ")}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                        </>
                    )}

                    <Typography
                        variant="h6"
                        sx={{ mb: 2, fontWeight: 600 }}
                    >
                        Horario semanal
                    </Typography>

                    {/* Weekly schedule */}
                    <TableContainer sx={studentScheduleSx}>
                        <Table
                            size="small"
                            sx={{ tableLayout: "fixed" }}
                        >
                            <TableHead>
                                <TableRow>
                                    <TableCell
                                        sx={{
                                            bgcolor: "var(--curriculum-blue)",
                                            color: "#FFFFFF",
                                            fontWeight: 600,
                                            width: 90
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
                                                textAlign: "center",
                                                fontWeight: 600
                                            }}
                                        >
                                            Bloque {block}
                                        </TableCell>

                                        {days.map(day => {

                                            const blockCode =
                                                `${day.code}${block}`;

                                            const course = courses.find(
                                                item =>
                                                    item.blocks.includes(
                                                        blockCode
                                                    )
                                            );

                                            return (
                                                <TableCell
                                                    key={blockCode}
                                                    align="center"
                                                    sx={{
                                                        height: 70,
                                                        p: 1,
                                                        bgcolor: course
                                                            ? "var(--in-progress-color)"
                                                            : "#FFFFFF"
                                                    }}
                                                >
                                                    {course && (
                                                        <Box>
                                                            <Typography
                                                                variant="body2"
                                                                fontWeight={600}
                                                            >
                                                                {course.subjectCode}
                                                            </Typography>

                                                            <Typography
                                                                variant="caption"
                                                            >
                                                                {course.subjectName}
                                                            </Typography>
                                                        </Box>
                                                    )}
                                                </TableCell>
                                            );

                                        })}
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
        </Box>
    );
}

export default StudentCourses;
