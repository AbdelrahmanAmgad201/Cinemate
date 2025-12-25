import api from "./api-client.jsx";

export async function getUserProfileApi({ userId }) {
    try {
        const response = await api.get(`/user/v1/profile/${userId}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message || (err?.raw?.response?.data?.message) || '', status: err?.status || err?.raw?.response?.status };
    }
}

export async function isUserFollowedApi({ userId }) {
    try {
        const response = await api.get(`/follow/v1/is-followed/${userId}`);
        return { success: true, data: response.data === true };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function followUserApi({ userId }) {
    try {
        const response = await api.post(`/follow/v1/follow/${userId}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function unfollowUserApi({ userId }) {
    try {
        const response = await api.post(`/follow/v1/unfollow/${userId}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function getIsPublicApi() {
    try {
        const response = await api.get(`/user/v1/is-public`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function setIsPublicApi({ isPublic }) {
    try {
        const response = await api.put(`/user/v1/is-public/${isPublic}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function getUserIsPublicApi({ userId }) {
    try {
        const response = await api.post(`/user/v1/is-public/${userId}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function getFollowersApi({ page = 0, size = 25 }) {
    try {
        const response = await api.get(`/follow/v1/followers?page=${page}&size=${size}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err?.response?.data?.message || err?.message || 'Failed to fetch followers' };
    }
}

export async function getFollowingApi({ page = 0, size = 25 }) {
    try {
        const response = await api.get(`/follow/v1/followings?page=${page}&size=${size}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err?.response?.data?.message || err?.message || 'Failed to fetch following' };
    }
}

