
import { Box, Button } from "@mui/material";
import PropTypes from "prop-types";

function NavigationMenu({ items, selected, onSelect }) {

    return (
        <Box
            component="nav"
            sx={{
                display: "flex",
                width: "100%",
                borderBottom: "1px solid var(--border-color)",
                backgroundColor: "#FFFFFF"
            }}
        >
            {items.map((item) => {

                const isActive = selected === item.path;

                return (
                    <Button
                        key={item.path}
                        onClick={() => onSelect(item.path)}
                        disableRipple
                        sx={{
                            flex: 1,
                            minWidth: 0,
                            height: 46,
                            borderRadius: 0,
                            textTransform: "none",
                            backgroundColor: "transparent",
                            "&:hover": {
                                backgroundColor: "transparent"
                            }
                        }}
                    >
                        <Box
                            component="span"
                            sx={{
                                display: "inline-flex",
                                alignItems: "center",
                                justifyContent: "center",
                                px: 2.5,
                                py: 0.75,
                                borderRadius: "999px",
                                whiteSpace: "nowrap",
                                fontSize: "0.875rem",
                                fontWeight: isActive ? 600 : 500,
                                color: isActive
                                    ? "#FFFFFF"
                                    : "var(--text-color)",
                                backgroundColor: isActive
                                    ? "var(--primary-color)"
                                    : "transparent",
                                transition: "all 0.2s ease"
                            }}
                        >
                            {item.label}
                        </Box>
                    </Button>
                );
            })}
        </Box>
    );
}

NavigationMenu.propTypes = {
    items: PropTypes.arrayOf(PropTypes.shape({
        label: PropTypes.string.isRequired,
        path: PropTypes.string.isRequired
    })).isRequired,
    selected: PropTypes.string.isRequired,
    onSelect: PropTypes.func.isRequired
};

export default NavigationMenu;
