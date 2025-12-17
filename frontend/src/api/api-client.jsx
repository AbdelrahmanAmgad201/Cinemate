import axios from "axios";
import  signOutApi from "./sign-out-api.jsx";
import {JWT} from "../constants/constants.jsx";


const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// Some defaults and base urls / properties to an axios inctance
const api = axios.create({
    baseURL: `${BASE_URL}/api`,
});

api.interceptors.request.use(config => {
    // Get token from local storage
    const token = localStorage.getItem(JWT.STORAGE_NAME);
    if (token) {
        // Adds the token header
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
})

api.interceptors.response.use(
    res => res,
    err => {

        const status = err.response?.status;
        const data = err.response?.data;

        if (status === 401) {
            // logout user if token has error
            signOutApi();
        }

        // now matches backend error design
        const message = data?.message || data?.error || err.message;

        return Promise.reject({
            success: false,
            status,
            message,
            path: data?.path,
            raw: err
        });
    }
);

export default api;