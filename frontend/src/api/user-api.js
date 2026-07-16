import api from "./api-client.js";

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
        const response = await api.get(`/follow/v1/${userId}`);
        return { success: true, data: response.data === true };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function followUserApi({ userId }) {
    try {
        const response = await api.put(`/follow/v1/${userId}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function unfollowUserApi({ userId }) {
    try {
        const response = await api.delete(`/follow/v1/${userId}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function setUserDataApi(details) {
    try {
        const response = await api.put('/user/v1/profile-data', details);
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

export async function updateAboutApi({ about }) {
    try {
        const response = await api.put('/user/v1/about', { about });
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function setIsPublicApi({ isPublic }) {
    try {
        const response = await api.put('/user/v1/is-public', isPublic, {
            headers: { 'Content-Type': 'application/json' },
        });
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function updateBirthDateApi({ birthDate }) {
    try {
        const response = await api.put('/user/v1/birth-date', { birthDate });
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function updateUserNameApi({ firstName, lastName }) {
    try {
        const response = await api.put('/user/v1/name', { firstName, lastName });
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

