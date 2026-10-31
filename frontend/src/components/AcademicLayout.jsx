import { Box } from "@mui/material";

import { useLocation, useNavigate } from "react-router-dom";

import AppHeader from "./AppHeader";
import NavigationMenu from "./NavigationMenu";

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

    return (
        <Box
            sx={{
                minHeight: "100vh",
                backgroundColor: "var(--background-color)"
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
                selected={location.pathname}
                onSelect={(path) => navigate(path)}
            />

            <Box
                component="main"
                sx={{
                    width: "100%",
                    px: 4,
                    py: 3
                }}
            >
                {children}
            </Box>
        </Box>
    );
}

export default AcademicLayout;
