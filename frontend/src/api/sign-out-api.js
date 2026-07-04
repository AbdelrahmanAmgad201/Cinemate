import api from './api-client.js';
import {PATHS, JWT} from "../constants/constants.jsx";


export default async function signOutApi() {
    try {
        await api.post('/auth/v1/logout');
    } catch (err) {
        console.log("Error revoking session on logout:", err);
    }
    sessionStorage.removeItem(JWT.STORAGE_NAME);
    window.location.href = PATHS.ROOT;
}

