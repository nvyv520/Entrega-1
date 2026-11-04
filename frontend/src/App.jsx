import { Alert } from "@mui/material";

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
import StudentAcademicRecords from "./pages/student/StudentAcademicRecords";
import AdminCareers from "./pages/admin/AdminCareers";
import AdminSubjects from "./pages/admin/AdminSubjects";
import AdminTeachers from "./pages/admin/AdminTeachers";
import AdminStudents from "./pages/admin/AdminStudents";
import AdminPeriods from "./pages/admin/AdminPeriods";
import AdminSections from "./pages/admin/AdminSections";
import AdminEnrollments from "./pages/admin/AdminEnrollments";
import TeacherAcademicProvider from "./components/teacher/TeacherAcademicProvider";
import TeacherSections from "./pages/teacher/TeacherSections";
import TeacherSectionView from "./pages/teacher/TeacherSectionView";
import TeacherProfile from "./pages/teacher/TeacherProfile";

// define frontend routes for each role
// API endpoints are called from pages and services
// Navigation options for each role
const navigationItems = {
    ADMIN: [
        { label: "Estudiantes", path: "/admin/students" },
        { label: "Docentes", path: "/admin/teachers" },
        { label: "Carreras y planes", path: "/admin" },
        { label: "Asignaturas", path: "/admin/subjects" },
        { label: "Períodos", path: "/admin/periods" },
        { label: "Secciones", path: "/admin/sections" },
        { label: "Inscripciones", path: "/admin/enrollments" }
    ],
    TEACHER: [
        { label: "Mis secciones", path: "/teacher" },
        { label: "Calificaciones", path: "/teacher/grades" },
        { label: "Estudiantes", path: "/teacher/students" }
    ],
    STUDENT: [
        { label: "Mis cursos", path: "/student/courses" },
        { label: "Inscripción", path: "/student/enrollments" },
        { label: "Plan de estudio", path: "/student/study-plan" },
        { label: "Mis calificaciones", path: "/student/grades" },
        { label: "Historial académico", path: "/student/history" }
    ]
};

const profileColumns = [
    ["firstName", "Nombre"],
    ["lastName", "Apellido"],
    ["email", "Correo"],
    ["status", "Estado"]
];

// Validate the resource before rendering the shared teacher detail.
function TeacherSectionRoute() {
    const { sectionId, resource } = useParams();
    if (!["students", "grades"].includes(resource) || !/^\d+$/.test(sectionId)) {
        return <Navigate to="/access-denied" replace />;
    }
    return <TeacherSectionView grades={resource === "grades"} />;
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
        STUDENT: "/student/courses"
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
                        element={<AdminCareers />}
                    />
                    <Route path="/admin/subjects" element={<AdminSubjects />} />
                    <Route path="/admin/teachers" element={<AdminTeachers />} />
                    <Route path="/admin/students" element={<AdminStudents />} />
                    <Route path="/admin/periods" element={<AdminPeriods />} />
                    <Route path="/admin/sections" element={<AdminSections />} />
                    <Route path="/admin/enrollments" element={<AdminEnrollments />} />
                </Route>

                {/* Teacher */}
                <Route element={<ProtectedRoute roles={["TEACHER"]} />}>
                    <Route element={<TeacherAcademicProvider />}>
                        <Route path="/teacher" element={<TeacherSections />} />
                        <Route path="/teacher/sections/:sectionId/:resource" element={<TeacherSectionRoute />} />
                        <Route path="/teacher/profile" element={<TeacherProfile />} />
                        <Route path="/teacher/grades" element={<TeacherSectionView grades />} />
                        <Route path="/teacher/students" element={<TeacherSectionView />} />
                    </Route>
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
                        element={<StudentAcademicRecords />}
                    />

                    <Route
                        path="/student/enrollments"
                        element={<StudentEnrollment />}
                    />

                    
                    <Route
                        path="/student/grades"
                        element={<StudentAcademicRecords gradesOnly />}
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
