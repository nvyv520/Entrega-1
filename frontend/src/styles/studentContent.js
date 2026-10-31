// Shared presentation for student views, including the generic academic tables.
export const studentContentSx = {
    fontSize: "15px",
    "& .MuiTypography-root": {
        fontSize: "15px"
    },
    "& .MuiTypography-h5": {
        fontSize: "24px"
    },
    "& .MuiTypography-h6": {
        fontSize: "20px"
    },
    "& .MuiTypography-caption": {
        fontSize: "14px"
    },
    "& .MuiTableCell-root": {
        fontSize: "14px"
    },
    "& .MuiAlert-root": {
        fontSize: "14px"
    },
    "& .MuiTableContainer-root": {
        border: "1px solid var(--border-color)",
        borderRadius: "10px",
        overflowX: "auto",
        overflowY: "hidden"
    },
    "& .MuiTable-root": {
        borderCollapse: "separate",
        borderSpacing: 0,
        borderRadius: "inherit"
    },
    "& .MuiTableHead-root .MuiTableCell-root": {
        backgroundColor: "var(--curriculum-blue)",
        color: "#FFFFFF",
        fontWeight: 600,
        fontSize: "15px"
    },
    "& .MuiTableHead-root tr:first-of-type > :first-child": {
        borderTopLeftRadius: "9px"
    },
    "& .MuiTableHead-root tr:first-of-type > :last-child": {
        borderTopRightRadius: "9px"
    },
    "& .MuiTableBody-root tr > :last-child": {
        borderRight: 0
    },
    "& .MuiTableBody-root tr:last-child > .MuiTableCell-root": {
        borderBottom: 0
    },
    "& .MuiTableBody-root tr:last-child > :first-child": {
        borderBottomLeftRadius: "9px"
    },
    "& .MuiTableBody-root tr:last-child > :last-child": {
        borderBottomRightRadius: "9px"
    },
    "& .MuiButton-root": {
        borderRadius: "999px",
        fontSize: "14px",
        px: 2.5
    }
};

export const studentNavigationSx = {
    "& nav .MuiButton-root > span": {
        fontSize: "15px"
    }
};
