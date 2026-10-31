import { Alert, Stack, Typography } from "@mui/material";

import {
    Navigate,
    Route,
    Routes,
    useNavigate,
    useParams
} from "react-router-dom";

import keycloak from "./auth/keycloak";
import ProtectedRoute from "./auth/ProtectedRoute";

import AcademicLayout from "./components/AcademicLayout";
import AcademicView from "./components/AcademicView";
import StudentStudyPlan from "./pages/student/StudentStudyPlan";
import StudentCourses from "./pages/student/StudentCourses";
import StudentEnrollment from "./pages/student/StudentEnrollment";

// Navigation options for each role
const navigationItems = {
    ADMIN: [
        { label: "Administración", path: "/admin" }
    ],
    TEACHER: [
        { label: "Mis secciones", path: "/teacher" },
        { label: "Calificaciones", path: "/teacher/grades" },
        { label: "Estudiantes", path: "/teacher/students" }
    ],
    STUDENT: [
        { label: "Inscripción", path: "/student/enrollments" },
        { label: "Plan de estudio", path: "/student/study-plan" },
        { label: "Calificaciones", path: "/student/grades" },
        { label: "Mis cursos", path: "/student/courses" }
    ]
};

const profileColumns = [
    ["firstName", "Nombre"],
    ["lastName", "Apellido"],
    ["email", "Correo"],
    ["status", "Estado"]
];

const gradeColumns = [
    ["enrollmentId", "Inscripción"],
    ["finalGrade", "Nota final"],
    ["result", "Resultado"]
];

const sectionColumns = [
    ["id", "Sección"],
    ["subjectId", "Asignatura"],
    ["academicPeriodId", "Período"],
    ["blocks", "Bloques"],
    ["capacity", "Cupos totales"],
    ["enrolledStudents", "Inscritos"]
];

// Details of a teacher's assigned section
function TeacherSectionView() {

    const { sectionId, resource } = useParams();

    if (!["students", "grades"].includes(resource) ||
        !/^\d+$/.test(sectionId)) {
        return <Navigate to="/access-denied" replace />;
    }

    const students = resource === "students";

    return (
        <AcademicView
            title={`${students ? "Estudiantes" : "Calificaciones"} de la sección ${sectionId}`}
            endpoint={
                students
                    ? `/sections/me/${sectionId}/students`
                    : `/grades/me/sections/${sectionId}`
            }
            columns={
                students
                    ? [
                        ["id", "ID"],
                        ["firstName", "Nombre"],
                        ["lastName", "Apellido"]
                    ]
                    : gradeColumns
            }
        />
    );
}

function App() {

    const navigate = useNavigate();

    // Get authenticated user role
    const role = ["ADMIN", "TEACHER", "STUDENT"]
        .find(currentRole => keycloak.hasRealmRole(currentRole));

    const items = navigationItems[role] || [];

    const roleNames = {
        ADMIN: "Administrador",
        TEACHER: "Docente",
        STUDENT: "Estudiante"
    };

    const name = keycloak.tokenParsed?.name ||
        keycloak.tokenParsed?.preferred_username ||
        "Usuario";

    const profilePaths = {
        ADMIN: "/admin",
        TEACHER: "/teacher/profile",
        STUDENT: "/student"
    };

    const defaultPath = {
        ADMIN: "/admin",
        TEACHER: "/teacher",
        STUDENT: "/student/enrollments"
    }[role] || "/access-denied";

    const openProfile = () => {
        navigate(profilePaths[role] || "/access-denied");
    };

    const logout = () => {
        keycloak.logout({
            redirectUri: window.location.origin + "/"
        });
    };

    return (
        <AcademicLayout
            name={name}
            role={roleNames[role] || "Sin rol"}
            items={items}
            onProfile={openProfile}
            onLogout={logout}
        >
            <Routes>

                <Route
                    path="/"
                    element={<Navigate to={defaultPath} replace />}
                />

                {/* Administrator */}
                <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
                    <Route
                        path="/admin"
                        element={
                            <AcademicView
                                title="Carreras"
                                endpoint="/careers"
                                columns={[
                                    ["code", "Código"],
                                    ["name", "Carrera"],
                                    ["status", "Estado"]
                                ]}
                            />
                        }
                    />
                </Route>

                {/* Teacher */}
                <Route element={<ProtectedRoute roles={["TEACHER"]} />}>

                    <Route
                        path="/teacher"
                        element={
                            <AcademicView
                                title="Mis secciones"
                                endpoint="/sections/me"
                                columns={sectionColumns}
                                sectionLinks
                            />
                        }
                    />

                    <Route
                        path="/teacher/sections/:sectionId/:resource"
                        element={<TeacherSectionView />}
                    />

                    <Route
                        path="/teacher/profile"
                        element={
                            <AcademicView
                                title="Mi perfil"
                                endpoint="/teachers/me"
                                columns={profileColumns}
                            />
                        }
                    />

                    <Route
                        path="/teacher/grades"
                        element={
                            <Alert severity="info">
                                Aquí incorporaremos la consulta y el registro de calificaciones.
                            </Alert>
                        }
                    />

                    <Route
                        path="/teacher/students"
                        element={
                            <Alert severity="info">
                                Aquí mostraremos los estudiantes de las secciones asignadas.
                            </Alert>
                        }
                    />

                </Route>

                {/* Student */}
                <Route element={<ProtectedRoute roles={["STUDENT"]} />}>

                    <Route
                        path="/student"
                        element={
                            <AcademicView
                                title="Mi perfil"
                                endpoint="/students/me"
                                columns={profileColumns}
                            />
                        }
                    />

                    <Route
                        path="/student/history"
                        element={
                            <AcademicView
                                title="Mi historial académico"
                                endpoint="/students/me/academic-history"
                                columns={[
                                    ["subjectCode", "Código"],
                                    ["subjectName", "Asignatura"],
                                    ["academicPeriodCode", "Período"],
                                    ["finalGrade", "Nota"],
                                    ["result", "Resultado"]
                                ]}
                            />
                        }
                    />

                    <Route
                        path="/student/enrollments"
                        element={<StudentEnrollment />}
                    />

                    
                    <Route
                        path="/student/grades"
                        element={
                            <AcademicView
                                title="Mis calificaciones"
                                endpoint="/students/me/academic-history"
                                columns={[
                                    ["subjectCode", "Código"],
                                    ["subjectName", "Asignatura"],
                                    ["academicPeriodCode", "Período académico"],
                                    ["finalGrade", "Nota final"],
                                    ["result", "Resultado"]
                                ]}
                            />
                        }
                    />


                    <Route
                        path="/student/study-plan"
                        element={<StudentStudyPlan />}
                    />

                    <Route
                        path="/student/courses"
                        element={<StudentCourses />}
                    />

                </Route>

                <Route
                    path="/access-denied"
                    element={
                        <Alert severity="error">
                            No tienes permisos para acceder a esta página.
                        </Alert>
                    }
                />

                <Route
                    path="*"
                    element={
                        <Alert severity="info">
                            Página no encontrada.
                        </Alert>
                    }
                />

            </Routes>
        </AcademicLayout>
    );
}

export default App;
