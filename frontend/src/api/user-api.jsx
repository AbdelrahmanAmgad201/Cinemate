import api from "./api-client.jsx";

export async function getUserProfileApi({ userId }) {
    try {
        const response = await api.get(`/user/v1/profile/${userId}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export default getUserProfileApi;
