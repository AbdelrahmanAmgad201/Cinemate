import api from './api-client.js';
import {PATHS} from "../constants/constants.jsx";
import {clearAccessToken} from "../auth/tokenStore.js";


export default async function signOutApi() {
    try {
        await api.post('/auth/v1/logout');
    } catch (err) {
        console.error("Error revoking session on logout:", err);
    }
    clearAccessToken();
    window.location.href = PATHS.ROOT;
}

