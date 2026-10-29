
import { Button, Container, Typography, Paper, Stack } from "@mui/material";
import keycloak from "./auth/keycloak";

function App() {

    const username = keycloak.tokenParsed?.preferred_username;

    const roles = keycloak.realmAccess?.roles || [];

    const appRoles = roles.filter(role =>
        ["ADMIN", "TEACHER", "STUDENT"].includes(role)
    );

    const logout = () => {
        keycloak.logout({
            redirectUri: "http://localhost:5173/"
        });
    };

    return (
        <Container maxWidth="sm" sx={{ mt: 8 }}>
            <Paper elevation={3} sx={{ p: 4 }}>
                <Stack spacing={3}>
                    <Typography variant="h4">
                        SIGA IPT
                    </Typography>

                    <Typography>
                        Bienvenido, {username}
                    </Typography>

                    <Typography>
                        Rol: {appRoles.join(", ") || "Sin rol asignado"}
                    </Typography>

                    <Button
                        variant="contained"
                        color="error"
                        onClick={logout}
                    >
                        Cerrar sesión
                    </Button>
                </Stack>
            </Paper>
        </Container>
    );
}

export default App;
