import { Alert, Box, Button, CircularProgress, Divider, Stack, Typography } from "@mui/material";
import { useTeacherRecords } from "../../services/teacherAcademic";

const fields = [["rut", "RUN"], ["firstName", "Nombre"], ["lastName", "Apellido"], ["email", "Correo"], ["professionalTitle", "Título profesional"], ["academicDegree", "Grado académico"], ["status", "Estado"]];

function TeacherProfile() {
    // GET /api/teachers/me
    const profile = useTeacherRecords("/teachers/me");
    return <Stack spacing={3}>
        <Typography variant="h5" fontWeight={600}>Mi perfil</Typography>
        {profile.loading ? <CircularProgress aria-label="Cargando perfil" /> : profile.error ? <Alert severity="error" action={<Button color="inherit" onClick={profile.reload}>Reintentar</Button>}>{profile.error}</Alert> : profile.rows[0] ? (
            <Box component="dl" sx={{ m: 0, maxWidth: 850, border: "1px solid var(--border-color)", borderRadius: "10px", px: 3 }}>
                {fields.map(([field, label], index) => <Box key={field}>
                    {index > 0 && <Divider />}
                    <Stack direction="row" spacing={3} sx={{ py: 1.5 }}>
                        <Typography component="dt" sx={{ width: 180, flexShrink: 0, fontWeight: 600 }}>{label}</Typography>
                        <Typography component="dd" sx={{ m: 0, overflowWrap: "anywhere" }}>{profile.rows[0][field] ?? "No informado"}</Typography>
                    </Stack>
                </Box>)}
            </Box>
        ) : <Alert severity="info">No se encontró el perfil docente.</Alert>}
    </Stack>;
}

export default TeacherProfile;
