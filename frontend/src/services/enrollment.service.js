
import http from "../http-common";

// get authenticated student
const getCurrentStudent = () => {
    // GET /api/students/me
    return http.get("/students/me");
};

// get open academic periods
const getOpenAcademicPeriods = () => {
    // GET /api/academic-periods/status/ABIERTO
    return http.get("/academic-periods/status/ABIERTO");
};

// get sections by academic period
const getSectionsByAcademicPeriod = (periodId) => {
    // GET /api/sections/academic-period/{periodId}
    return http.get(`/sections/academic-period/${periodId}`);
};

// Academic eligibility is decided by the backend for the authenticated student.
const getEligibleSections = (periodId) => {
    // GET /api/enrollments/me/academic-period/{periodId}/eligible-sections
    return http.get(`/enrollments/me/academic-period/${periodId}/eligible-sections`);
};

// get subjects by study plan
const getSubjectsByStudyPlan = (studyPlanId) => {
    // GET /api/subjects/study-plan/{studyPlanId}
    return http.get(`/subjects/study-plan/${studyPlanId}`);
};

// get authenticated student enrollments
const getCurrentEnrollments = (selection = false) => {
    // GET /api/enrollments/me?selection={selection}
    return http.get("/enrollments/me", { params: { selection } });
};

// enroll authenticated student in a section
const createEnrollment = (sectionId) => {
    // POST /api/enrollments/me?sectionId={sectionId}
    return http.post(`/enrollments/me?sectionId=${sectionId}`);
};

// cancel an enrollment
const cancelEnrollment = (enrollmentId) => {
    // PATCH /api/enrollments/me/{enrollmentId}/cancel
    return http.patch(`/enrollments/me/${enrollmentId}/cancel`);
};

// verify the student selection
const verifySelection = (periodId) => {
    // POST /api/enrollments/me/academic-period/{periodId}/verify
    return http.post(`/enrollments/me/academic-period/${periodId}/verify`);
};

// discard the provisional selection
const discardSelection = (periodId) => {
    // POST /api/enrollments/me/academic-period/{periodId}/discard
    return http.post(`/enrollments/me/academic-period/${periodId}/discard`);
};

// get the selection confirmation status
const getVerificationStatus = (periodId) => {
    // GET /api/enrollments/me/academic-period/{periodId}/verification-status
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
