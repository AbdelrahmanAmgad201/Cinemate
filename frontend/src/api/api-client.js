import axios from "axios";
import  signOutApi from "./sign-out-api.js";
import {BACKEND_URL, JWT} from "../constants/constants.jsx";

// Some defaults and base urls / properties to an axios instance
const api = axios.create({
    baseURL: `${BACKEND_URL.BASE_URL}/api`,
    // Send the httpOnly refresh cookie. It's Path-scoped to /api/auth/v1 on the
    // server, so it only actually rides along on /refresh and /logout.
    withCredentials: true,
});

api.interceptors.request.use(config => {
    // Attach the short-lived access token from session storage.
    const token = sessionStorage.getItem(JWT.STORAGE_NAME);
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
})

// Single-flight refresh: if several requests 401 at once (access token just
// expired), they all await ONE /refresh call instead of stampeding the endpoint
// and rotating the refresh token multiple times.
let refreshPromise = null;

export function refreshAccessToken() {
    if (!refreshPromise) {
        refreshPromise = axios
            .post(`${BACKEND_URL.BASE_URL}/api/auth/v1/refresh`, {}, { withCredentials: true })
            .then(res => {
                const token = res.data.accessToken;
                sessionStorage.setItem(JWT.STORAGE_NAME, token);
                return token;
            })
            .finally(() => { refreshPromise = null; });
    }
    return refreshPromise;
}

api.interceptors.response.use(
    res => res,
    async err => {
        const original = err.config;
        const status = err.response?.status;

        // On a 401, transparently try to mint a fresh access token from the refresh
        // cookie and replay the request — but only once per request, and never for
        // the refresh call itself, to avoid an infinite loop.
        if (status === 401 && original && !original._retry &&
            !original.url?.includes("/auth/v1/refresh")) {
            original._retry = true;
            try {
                const newToken = await refreshAccessToken();
                original.headers = original.headers || {};
                original.headers.Authorization = `Bearer ${newToken}`;
                return api(original);
            } catch {
                // Refresh failed → the session is really over.
                signOutApi();
            }
        }

        const data = err.response?.data;

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
