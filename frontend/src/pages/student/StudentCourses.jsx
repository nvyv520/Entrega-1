
import { useEffect, useState } from "react";

import {
    Alert,
    Box,
    CircularProgress,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography
} from "@mui/material";

import http from "../../http-common";

const days = [
    { code: "L", label: "Lunes" },
    { code: "M", label: "Martes" },
    { code: "W", label: "Miércoles" },
    { code: "J", label: "Jueves" },
    { code: "V", label: "Viernes" }
];

const blocks = [1, 2, 3, 4, 5, 6];

function StudentCourses() {

    const [courses, setCourses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {

        let active = true;

        const loadCourses = async () => {

            try {

                // Get authenticated student enrollments
                const enrollmentsResponse =
                    await http.get("/enrollments/me");

                const activeEnrollments =
                    enrollmentsResponse.data.filter(
                        enrollment => enrollment.status === "ACTIVO"
                    );

                // Get sections corresponding to enrollments
                const sectionIds = [
                    ...new Set(
                        activeEnrollments.map(
                            enrollment => enrollment.sectionId
                        )
                    )
                ];

                const sections = await Promise.all(
                    sectionIds.map(async sectionId => {
                        const response =
                            await http.get(`/sections/${sectionId}`);

                        return response.data;
                    })
                );

                // Get subject information
                const subjectIds = [
                    ...new Set(
                        sections.map(section => section.subjectId)
                    )
                ];

                const subjects = await Promise.all(
                    subjectIds.map(async subjectId => {
                        const response =
                            await http.get(`/subjects/${subjectId}`);

                        return response.data;
                    })
                );

                const coursesData = activeEnrollments.map(enrollment => {

                    const section = sections.find(
                        item => item.id === enrollment.sectionId
                    );

                    const subject = subjects.find(
                        item => item.id === section?.subjectId
                    );

                    return {
                        enrollmentId: enrollment.id,
                        sectionId: enrollment.sectionId,
                        subjectCode: subject?.code || "",
                        subjectName: subject?.name || "",
                        blocks: section?.blocks || []
                    };

                });

                if (active) {
                    setCourses(coursesData);
                }

            } catch (error) {

                console.error("Error loading courses:", error);

                if (active) {
                    setError("No se pudieron cargar los cursos");
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

    }, []);

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

            <Typography
                variant="h5"
                sx={{ fontWeight: 600, mb: 3 }}
            >
                Mis cursos
            </Typography>

            <Typography
                variant="h6"
                sx={{ mb: 2, fontWeight: 600 }}
            >
                Asignaturas inscritas
            </Typography>

            {courses.length === 0 ? (
                
            <Alert severity="info" sx={{ mb: 3 }}>
                No tienes asignaturas inscritas actualmente.
            </Alert>
            
            ) : (
                <>

                    {/* Enrolled courses table */}
                    <TableContainer
                        sx={{
                            border: "1px solid var(--border-color)",
                            borderRadius: "3px",
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
                                            {course.sectionId}
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
                    <TableContainer
                        sx={{
                            border: "1px solid var(--border-color)",
                            borderRadius: "3px"
                        }}
                    >
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
                                                            : "#FFFFFF",
                                                        border:
                                                            "1px solid var(--border-color)"
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
