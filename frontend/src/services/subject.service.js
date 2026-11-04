import http from "../http-common";

// get authenticated student
const getCurrentStudent = () => {
    // GET /api/students/me
    return http.get("/students/me");
};

// get subjects by study plan
const getSubjectsByStudyPlan = (studyPlanId) => {
    // GET /api/subjects/study-plan/{studyPlanId}
    return http.get(`/subjects/study-plan/${studyPlanId}`);
};

// get authenticated student academic history
const getCurrentStudentAcademicHistory = () => {
    // GET /api/students/me/academic-history
    return http.get("/students/me/academic-history");
};

// get authenticated student enrollments
const getCurrentStudentEnrollments = () => {
    // GET /api/enrollments/me
    return http.get("/enrollments/me");
};

// get study plan by id
const getStudyPlanById = (studyPlanId) => {
    // GET /api/study-plans/{studyPlanId}
    return http.get(`/study-plans/${studyPlanId}`);
};

// get career by id
const getCareerById = (careerId) => {
    // GET /api/careers/{careerId}
    return http.get(`/careers/${careerId}`);
};

const subjectService = {
    getCurrentStudent,
    getSubjectsByStudyPlan,
    getCurrentStudentAcademicHistory,
    getCurrentStudentEnrollments,
    getStudyPlanById,
    getCareerById
};
export default subjectService;
