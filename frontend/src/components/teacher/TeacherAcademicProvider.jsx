import { Outlet } from "react-router-dom";
import { TeacherContext, useTeacherRecords } from "../../services/teacherAcademic";
import { firstOpenAcademicPeriod } from "../../services/studentAcademic";

// load the teacher sections and their academic information
function TeacherAcademicProvider() {
    // GET /api/sections/me
    const assigned = useTeacherRecords("/sections/me");
    // GET /api/subjects
    const subjects = useTeacherRecords("/subjects");
    // GET /api/academic-periods
    const periods = useTeacherRecords("/academic-periods");
    const sources = [assigned, subjects, periods];
    const failed = sources.find(source => source.error);
    const sections = assigned.rows.map(section => {
        const subject = subjects.rows.find(subject => subject.id === section.subjectId);
        const period = periods.rows.find(period => period.id === section.academicPeriodId);
        return {
            ...section,
            subjectCode: subject?.code || "No disponible",
            subjectName: subject?.name || "Asignatura no disponible",
            periodCode: period?.code || "Período no disponible",
            periodStatus: period?.status || null
        };
    });
    const value = {
        sections,
        periods: periods.rows,
        currentPeriod: firstOpenAcademicPeriod(periods.rows),
        loading: sources.some(source => source.loading),
        error: failed?.error || "",
        reload: () => sources.forEach(source => source.reload())
    };

    return <TeacherContext.Provider value={value}><Outlet /></TeacherContext.Provider>;
}

export default TeacherAcademicProvider;
