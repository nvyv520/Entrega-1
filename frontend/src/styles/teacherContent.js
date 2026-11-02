import { studentContentSx } from "./studentContent";

// Reuse SIGA tables and spacing without changing other roles' styles.
export const teacherContentSx = {
    ...studentContentSx,
    "& .MuiTableCell-root": { fontSize: "15px" },
    "& .MuiTypography-h5": { fontSize: "24px", color: "var(--curriculum-blue)" },
    "& .MuiTypography-h6, & .MuiDialogTitle-root": { fontSize: "20px", color: "var(--curriculum-blue)" },
    "& .MuiInputBase-root": { fontSize: "15px" },
    "& .MuiInputLabel-root": { fontSize: "14px" },
    "& .MuiFormHelperText-root": { fontSize: "13px" },
    "& .MuiButton-colorPrimary:not(.Mui-disabled)": { color: "var(--curriculum-blue)" },
    "& .MuiButton-outlined.MuiButton-colorPrimary:not(.Mui-disabled)": { borderColor: "var(--curriculum-blue)" },
    "& .MuiButton-contained.MuiButton-colorPrimary:not(.Mui-disabled)": { backgroundColor: "var(--curriculum-blue)", color: "#FFFFFF" },
    "& .MuiInputLabel-root.Mui-focused:not(.Mui-error)": { color: "var(--curriculum-blue)" },
    "& .MuiOutlinedInput-root.Mui-focused:not(.Mui-error) .MuiOutlinedInput-notchedOutline": { borderColor: "var(--curriculum-blue)" }
};
