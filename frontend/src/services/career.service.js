import http from "../http-common";

// get all careers
const getAllCareers = () => {
    // GET /api/careers
    return http.get("/careers");
};

const careerService = {
    getAllCareers
};

export default careerService;