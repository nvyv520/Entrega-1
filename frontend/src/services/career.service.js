import http from "../http-common";

const getAllCareers = () => {
    return http.get("/careers");
};

const careerService = {
    getAllCareers
};

export default careerService;