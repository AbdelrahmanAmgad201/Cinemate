import axios from "axios";
import  signOutApi from "./sign-out-api.jsx";
import {BACKEND_URL, JWT} from "../constants/constants.jsx";

// Some defaults and base urls / properties to an axios inctance
const api = axios.create({
    baseURL: `${BACKEND_URL.BASE_URL}/api`,
});

api.interceptors.request.use(config => {
    // Get token from local storage
    const token = sessionStorage.getItem(JWT.STORAGE_NAME);
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