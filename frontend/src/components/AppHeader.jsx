import { useState } from "react";

import {
    AppBar,
    Avatar,
    Box,
    Button,
    Divider,
    Menu,
    MenuItem,
    Toolbar,
    Typography
} from "@mui/material";

import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import PropTypes from "prop-types";

function AppHeader({ name, role, onProfile, onLogout }) {

    const [anchorEl, setAnchorEl] = useState(null);

    const menuOpen = Boolean(anchorEl);

    const openMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const closeMenu = () => {
        setAnchorEl(null);
    };

    const handleProfile = () => {
        closeMenu();
        onProfile();
    };

    const handleLogout = () => {
        closeMenu();
        onLogout();
    };

    return (
        <AppBar
            position="static"
            elevation={0}
            sx={{
                backgroundColor: "var(--primary-color)",
                color: "#FFFFFF",
                borderBottom: "1px solid var(--border-color)"
            }}
        >
            <Toolbar
                sx={{
                    minHeight: "70px !important",
                    px: 3,
                    display: "flex",
                    justifyContent: "space-between"
                }}
            >
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <Typography
                        variant="h4"
                        sx={{
                            fontWeight: 700,
                            color: "#FFFFFF"
                        }}
                    >
                        SIGA
                    </Typography>

                    <Divider
                        orientation="vertical"
                        flexItem
                        sx={{
                            borderColor: "#FFFFFF",
                            opacity: 0.7
                        }}
                    />

                    <Typography
                        variant="body2"
                        sx={{ lineHeight: 1.3 }}
                    >
                        Sistema Integrado
                        <br />
                        de Gestión Académica
                    </Typography>
                </Box>

                <Button
                    onClick={openMenu}
                    color="inherit"
                    sx={{
                        textTransform: "none",
                        borderRadius: "var(--border-radius)",
                        gap: 1.5
                    }}
                >
                    
                    <Box
                        sx={{
                            display: "flex",
                            flexDirection: "column",
                            alignItems: "flex-end",
                            gap: 0
                        }}
                    >
                        <Typography
                            sx={{
                                fontSize: "1rem",
                                fontWeight: 700,
                                lineHeight: 1.1,
                                color: "#FFFFFF"
                            }}
                        >
                            {name?.toLocaleLowerCase("es-CL").replace(
                                /(^|[\s-])\p{L}/gu,
                                letter => letter.toLocaleUpperCase("es-CL")
                            )}
                        </Typography>

                        <Typography
                            sx={{
                                fontSize: "0.75rem",
                                lineHeight: 1.1,
                                color: "#E9EFFA"
                            }}
                        >
                            {role}
                        </Typography>
                    </Box>


                    <Avatar
                        sx={{
                            bgcolor: "var(--curriculum-blue)",
                            color: "#FFFFFF",
                            width: 40,
                            height: 40,
                            border: "1px solid #FFFFFF",
                            fontWeight: 600,
                            fontSize: "1rem"
                        }}
                    >
                        {name?.charAt(0).toUpperCase() || "U"}
                    </Avatar>

                    <KeyboardArrowDownIcon fontSize="small" />
                </Button>

                <Menu
                    anchorEl={anchorEl}
                    open={menuOpen}
                    onClose={closeMenu}
                    slotProps={{
                        paper: {
                            sx: {
                                borderRadius: "var(--border-radius)",
                                minWidth: 170
                            }
                        }
                    }}
                >
                    <MenuItem onClick={handleProfile}>
                        Mi perfil
                    </MenuItem>

                    <Divider />

                    <MenuItem onClick={handleLogout}>
                        Cerrar sesión
                    </MenuItem>
                </Menu>
            </Toolbar>
        </AppBar>
    );
}

AppHeader.propTypes = {
    name: PropTypes.string.isRequired,
    role: PropTypes.string.isRequired,
    onProfile: PropTypes.func.isRequired,
    onLogout: PropTypes.func.isRequired
};

export default AppHeader;
