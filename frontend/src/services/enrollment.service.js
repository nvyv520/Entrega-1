
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

// Academic eligibility is decided by the backend for the authenticated student.
const getEligibleSections = (periodId) => {
    return http.get(`/enrollments/me/academic-period/${periodId}/eligible-sections`);
};

// get subjects by study plan
const getSubjectsByStudyPlan = (studyPlanId) => {
    return http.get(`/subjects/study-plan/${studyPlanId}`);
};

// get authenticated student enrollments
const getCurrentEnrollments = (selection = false) => {
    return http.get("/enrollments/me", { params: { selection } });
};

// enroll authenticated student in a section
const createEnrollment = (sectionId) => {
    return http.post(`/enrollments/me?sectionId=${sectionId}`);
};

// cancel an enrollment
const cancelEnrollment = (enrollmentId) => {
    return http.patch(`/enrollments/me/${enrollmentId}/cancel`);
};

const verifySelection = (periodId) => {
    return http.post(`/enrollments/me/academic-period/${periodId}/verify`);
};

const discardSelection = (periodId) => {
    return http.post(`/enrollments/me/academic-period/${periodId}/discard`);
};

const getVerificationStatus = (periodId) => {
    return http.get(
        `/enrollments/me/academic-period/${periodId}/verification-status`
    );
};


const enrollmentService = {
    getCurrentStudent,
    getOpenAcademicPeriods,
    getSectionsByAcademicPeriod,
    getEligibleSections,
    getSubjectsByStudyPlan,
    getCurrentEnrollments,
    createEnrollment,
    cancelEnrollment,
    verifySelection,
    discardSelection,
    getVerificationStatus,
};

export default enrollmentService;
