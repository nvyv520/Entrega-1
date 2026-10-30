import PropTypes from "prop-types";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { Alert, Button, Stack } from "@mui/material";
import keycloak from "./keycloak";

function ProtectedRoute({ roles }) {
    const location = useLocation();
    if (!keycloak.authenticated) {
        return <Stack spacing={2}>
            <Alert severity="info">Inicia sesión para continuar.</Alert>
            <Button onClick={() => keycloak.login({ redirectUri: window.location.href })}>Iniciar sesión</Button>
        </Stack>;
    }
    if (!roles.some(role => keycloak.hasRealmRole(role))) {
        return <Navigate to="/access-denied" state={{ from: location.pathname }} replace />;
    }
    return <Outlet />;
}

ProtectedRoute.propTypes = { roles: PropTypes.arrayOf(PropTypes.string).isRequired };
export default ProtectedRoute;
