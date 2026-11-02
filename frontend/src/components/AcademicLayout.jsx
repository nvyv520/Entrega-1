import { Box } from "@mui/material";

import { useLocation, useNavigate } from "react-router-dom";

import AppHeader from "./AppHeader";
import NavigationMenu from "./NavigationMenu";
import { studentContentSx, studentNavigationSx } from "../styles/studentContent";
import { adminContentSx } from "../styles/adminContent";
import { teacherContentSx } from "../styles/teacherContent";
import PropTypes from "prop-types";

function AcademicLayout({
    name,
    role,
    items,
    onProfile,
    onLogout,
    children
}) {

    const navigate = useNavigate();
    const location = useLocation();
    const teacherDetail = role === "Docente" && /^\/teacher\/sections\/\d+\/(grades|students)$/.test(location.pathname);
    const selectedPath = teacherDetail ? `/teacher/${location.pathname.split("/").pop()}` : location.pathname;

    return (
        <Box
            sx={{
                minHeight: "100vh",
                backgroundColor: "var(--background-color)",
                ...(["Estudiante", "Administrador", "Docente"].includes(role) ? studentNavigationSx : {})
            }}
        >
            <AppHeader
                name={name}
                role={role}
                onProfile={onProfile}
                onLogout={onLogout}
            />

            <NavigationMenu
                items={items}
                selected={selectedPath}
                onSelect={(path) => navigate(path)}
            />

            <Box
                component="main"
                sx={{
                    width: "100%",
                    px: 4,
                    py: 3,
                    ...(role === "Administrador" ? adminContentSx : role === "Estudiante" ? studentContentSx : role === "Docente" ? teacherContentSx : {})
                }}
            >
                {children}
            </Box>
        </Box>
    );
}

AcademicLayout.propTypes = {
    name: PropTypes.string.isRequired,
    role: PropTypes.string.isRequired,
    items: PropTypes.arrayOf(PropTypes.shape({
        label: PropTypes.string.isRequired,
        path: PropTypes.string.isRequired
    })).isRequired,
    onProfile: PropTypes.func.isRequired,
    onLogout: PropTypes.func.isRequired,
    children: PropTypes.node.isRequired
};

export default AcademicLayout;
