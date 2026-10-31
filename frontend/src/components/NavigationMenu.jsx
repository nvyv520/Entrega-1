import { Box, Button } from "@mui/material";

function NavigationMenu({ items, selected, onSelect }) {

    return (
        <Box
            component="nav"
            sx={{
                display: "flex",
                width: "100%",
                borderBottom: "1px solid var(--border-color)"
            }}
        >
            {items.map((item) => (
                <Button
                    key={item.path}
                    onClick={() => onSelect(item.path)}
                    sx={{
                        flex: 1,
                        minWidth: 0,
                        height: 38,
                        borderRadius: 0,
                        borderRight: "1px solid var(--border-color)",
                        textTransform: "none",
                        fontSize: "0.875rem",
                        fontWeight: 500,
                        color: selected === item.path
                            ? "#FFFFFF"
                            : "var(--text-color)",
                        backgroundColor: selected === item.path
                            ? "var(--primary-color)"
                            : "#FFFFFF",
                        "&:hover": {
                            backgroundColor: selected === item.path
                                ? "var(--curriculum-blue)"
                                : "#F2F5FA"
                        }
                    }}
                >
                    {item.label}
                </Button>
            ))}
        </Box>
    );
}

export default NavigationMenu;
