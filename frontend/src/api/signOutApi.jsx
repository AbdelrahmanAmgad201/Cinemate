import api from './apiClient.jsx';


export default async function signOutApi() {
    localStorage.removeItem('token');
    window.location.href = "/";
}

