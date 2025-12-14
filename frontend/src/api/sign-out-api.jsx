import api from './api-client.jsx';
import {PATHS, JWT} from "../constants/constants.jsx";


export default async function signOutApi() {
    sessionStorage.removeItem(JWT.STORAGE_NAME);
    window.location.href = PATHS.ROOT;
}

