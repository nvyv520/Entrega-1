import { studentContentSx } from "./studentContent";

// Keep SIGA's presentation while adjusting typography only inside ADMIN.
export const adminContentSx = {
    ...studentContentSx,
    fontSize: "16px",
    "& .MuiTypography-root": {
        fontSize: "16px"
    },
    "& .MuiTypography-h5": {
        fontSize: "24px"
    },
    "& .MuiTypography-h6, & .MuiDialogTitle-root": {
        fontSize: "20px"
    },
    "& .MuiTypography-subtitle1": {
        fontSize: "18px"
    },
    "& .MuiTypography-subtitle2": {
        fontSize: "16px"
    },
    "& .MuiTypography-body1": {
        fontSize: "16px"
    },
    "& .MuiTypography-body2": {
        fontSize: "15px"
    },
    "& .MuiTableCell-root": {
        fontSize: "15px"
    },
    "& .MuiInputBase-root": {
        fontSize: "15px"
    },
    "& .MuiInputLabel-root": {
        fontSize: "14px"
    },
    "& .MuiFormHelperText-root": {
        fontSize: "13px"
    },
    "& .MuiTypography-h5, & .MuiTypography-h6, & .MuiDialogTitle-root": {
        color: "var(--curriculum-blue)"
    },
    "& .MuiButton-colorPrimary:not(.Mui-disabled), & .MuiLink-root, & .MuiTab-root.Mui-selected": {
        color: "var(--curriculum-blue)"
    },
    "& .MuiButton-outlined.MuiButton-colorPrimary:not(.Mui-disabled)": {
        borderColor: "var(--curriculum-blue)"
    },
    "& .MuiButton-contained.MuiButton-colorPrimary:not(.Mui-disabled)": {
        backgroundColor: "var(--curriculum-blue)",
        color: "#FFFFFF",
        "&:hover": { backgroundColor: "var(--curriculum-blue)" }
    },
    "& .MuiTabs-indicator": {
        backgroundColor: "var(--curriculum-blue)"
    },
    "& .MuiInputLabel-root.Mui-focused:not(.Mui-error), & .MuiCheckbox-colorPrimary.Mui-checked:not(.Mui-disabled)": {
        color: "var(--curriculum-blue)"
    },
    "& .MuiOutlinedInput-root.Mui-focused:not(.Mui-error) .MuiOutlinedInput-notchedOutline": {
        borderColor: "var(--curriculum-blue)"
    }
};
