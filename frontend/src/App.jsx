import { Button, Container, Typography, Paper, Stack, Alert } from "@mui/material";
import { Link, Navigate, Route, Routes, useParams } from "react-router-dom";
import keycloak from "./auth/keycloak";
import ProtectedRoute from "./auth/ProtectedRoute";
import AcademicView from "./components/AcademicView";

const rolePages = [
    { role: "ADMIN", path: "/admin", label: "Administración" },
    { role: "TEACHER", path: "/teacher", label: "Docencia" },
    { role: "STUDENT", path: "/student", label: "Estudiante" }
];
const profileColumns = [["firstName", "Nombre"], ["lastName", "Apellido"], ["email", "Correo"], ["status", "Estado"]];
const gradeColumns = [["enrollmentId", "Inscripción"], ["finalGrade", "Nota final"], ["result", "Resultado"]];
const sectionColumns = [["id", "Sección"], ["subjectId", "Asignatura"], ["academicPeriodId", "Período"], ["blocks", "Horario"], ["capacity", "Cupos totales"], ["enrolledStudents", "Inscritos"]];

function TeacherSectionView() {
    const { sectionId, resource } = useParams();
    if (!["students", "grades"].includes(resource) || !/^\d+$/.test(sectionId)) {
        return <Navigate to="/access-denied" replace />;
    }
    const students = resource === "students";
    return <AcademicView
        title={`${students ? "Estudiantes" : "Calificaciones"} de la sección ${sectionId}`}
        endpoint={students ? `/sections/me/${sectionId}/students` : `/grades/me/sections/${sectionId}`}
        columns={students ? profileColumns : gradeColumns}
    />;
}

function App() {
    const pages = rolePages.filter(({ role }) => keycloak.hasRealmRole(role));
    const defaultPath = pages[0]?.path || "/access-denied";
    return <Container maxWidth="lg" sx={{ py: 4 }}>
        <Stack spacing={3}>
            <Paper sx={{ p: 3 }}><Stack spacing={2}>
                <Typography variant="h4">SIGA IPT</Typography>
                <Typography>Bienvenido, {keycloak.tokenParsed?.preferred_username}</Typography>
                <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: "wrap" }}>
                    {pages.map(page => <Button key={page.role} component={Link} to={page.path}>{page.label}</Button>)}
                    <Button color="error" onClick={() => keycloak.logout({ redirectUri: window.location.origin + "/" })}>Cerrar sesión</Button>
                </Stack>
            </Stack></Paper>
            <Routes>
                <Route path="/" element={<Navigate to={defaultPath} replace />} />
                <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
                    <Route path="/admin" element={<Stack spacing={2}>
                        <Typography variant="h5">Administración académica</Typography>
                        <Typography>Consulta las carreras registradas en SIGA.</Typography>
                        <AcademicView title="Carreras" endpoint="/careers" columns={[["code", "Código"], ["name", "Carrera"], ["status", "Estado"]]} />
                    </Stack>} />
                </Route>
                <Route element={<ProtectedRoute roles={["TEACHER"]} />}>
                    <Route path="/teacher" element={<Stack spacing={2}>
                        <Typography variant="h5">Portal docente</Typography>
                        <AcademicView title="Mis secciones" endpoint="/sections/me" columns={sectionColumns} sectionLinks />
                    </Stack>} />
                    <Route path="/teacher/sections/:sectionId/:resource" element={<TeacherSectionView />} />
                </Route>
                <Route element={<ProtectedRoute roles={["STUDENT"]} />}>
                    <Route path="/student" element={<Stack spacing={2}>
                        <Typography variant="h5">Portal del estudiante</Typography>
                        <Stack direction="row" spacing={1}>
                            <Button component={Link} to="/student/history">Historial</Button>
                            <Button component={Link} to="/student/enrollments">Inscripciones</Button>
                            <Button component={Link} to="/student/grades">Calificaciones</Button>
                        </Stack>
                        <AcademicView title="Mi perfil" endpoint="/students/me" columns={profileColumns} />
                    </Stack>} />
                    <Route path="/student/history" element={<AcademicView title="Mi historial" endpoint="/students/me/academic-history" columns={[["subjectCode", "Código"], ["subjectName", "Asignatura"], ["academicPeriodCode", "Período"], ["finalGrade", "Nota"], ["result", "Resultado"]]} />} />
                    <Route path="/student/enrollments" element={<AcademicView title="Mis inscripciones" endpoint="/enrollments/me" columns={[["id", "Inscripción"], ["sectionId", "Sección"], ["academicPeriodId", "Período"], ["status", "Estado"]]} />} />
                    <Route path="/student/grades" element={<AcademicView title="Mis calificaciones" endpoint="/grades/me" columns={gradeColumns} />} />
                </Route>
                <Route path="/access-denied" element={<Alert severity="error">Acceso denegado. Tu cuenta no tiene el rol necesario para esta página.</Alert>} />
                <Route path="*" element={<Alert severity="info">Página no encontrada. Selecciona un portal en el menú.</Alert>} />
            </Routes>
        </Stack>
    </Container>;
}

export default App;
