import axios from "axios";
import  signOutApi from "./signOutApi";

// Some defaults and base urls / properties to an axios inctance
const api = axios.create({
    baseURL: "http://localhost:8080/api",
});

api.interceptors.request.use(config => {
    // Get token from local storage
    const token = localStorage.getItem("token");
    if (token) {
        // Adds the token header
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
})

api.interceptors.response.use(
    res => res,
    err => {
        if (err.response?.status === 401) {
            // logout user if token has error
            signOutApi();
        }
        return Promise.reject(err);
    }
);

export default api;