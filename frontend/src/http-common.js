
import axios from "axios";
import keycloak from "./auth/keycloak";

// set the backend URL; service endpoints are relative to /api
const http = axios.create({
    baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
    headers: {
        "Content-Type": "application/json"
    }
});

// Add JWT to every request
http.interceptors.request.use(async (config) => {

    try {
        await keycloak.updateToken(30);
    } catch (error) {
        console.error("Could not refresh Keycloak token", error);
        await keycloak.login();
        return Promise.reject(error);
    }

    if (keycloak.token) {
        config.headers.Authorization = `Bearer ${keycloak.token}`;
    }

    return config;
});

export default http;
