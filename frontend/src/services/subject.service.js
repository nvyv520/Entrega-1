import http from "../http-common";

// get authenticated student
const getCurrentStudent = () => {
    return http.get("/students/me");
};

// get subjects by study plan
const getSubjectsByStudyPlan = (studyPlanId) => {
    return http.get(`/subjects/study-plan/${studyPlanId}`);
};

// get authenticated student academic history
const getCurrentStudentAcademicHistory = () => {
    return http.get("/students/me/academic-history");
};

// get authenticated student enrollments
const getCurrentStudentEnrollments = () => {
    return http.get("/enrollments/me");
};

// get study plan by id
const getStudyPlanById = (studyPlanId) => {
    return http.get(`/study-plans/${studyPlanId}`);
};

// get career by id
const getCareerById = (careerId) => {
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
