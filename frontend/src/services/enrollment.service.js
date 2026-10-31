
import http from "../http-common";

// get authenticated student
const getCurrentStudent = () => {
    return http.get("/students/me");
};

// get open academic periods
const getOpenAcademicPeriods = () => {
    return http.get("/academic-periods/status/ABIERTO");
};

// get sections by academic period
const getSectionsByAcademicPeriod = (periodId) => {
    return http.get(`/sections/academic-period/${periodId}`);
};

// get subjects by study plan
const getSubjectsByStudyPlan = (studyPlanId) => {
    return http.get(`/subjects/study-plan/${studyPlanId}`);
};

// get authenticated student enrollments
const getCurrentEnrollments = () => {
    return http.get("/enrollments/me");
};

// enroll authenticated student in a section
const createEnrollment = (sectionId) => {
    return http.post(`/enrollments/me?sectionId=${sectionId}`);
};

// cancel an enrollment
const cancelEnrollment = (enrollmentId) => {
    return http.patch(`/enrollments/me/${enrollmentId}/cancel`);
};

// confirm enrollment selection for academic period
const confirmSelection = (periodId) => {
    return http.patch(
        `/enrollments/me/academic-period/${periodId}/confirm`
    );
};

const enrollmentService = {
    getCurrentStudent,
    getOpenAcademicPeriods,
    getSectionsByAcademicPeriod,
    getSubjectsByStudyPlan,
    getCurrentEnrollments,
    createEnrollment,
    cancelEnrollment,
    confirmSelection
};

export default enrollmentService;
