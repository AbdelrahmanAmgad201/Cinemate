import api from './apiClient.jsx';
import {PATHS, JWT} from "../constants/constants.jsx";


export default async function signOutApi() {
    localStorage.removeItem(JWT.STORAGE_NAME);
    window.location.href = PATHS.ROOT;
}

