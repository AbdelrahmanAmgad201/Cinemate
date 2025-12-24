import api from "./api-client.jsx";

export async function getUserProfileApi({ userId }) {
    try {
        const response = await api.get(`/user/v1/profile/${userId}`);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
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

export async function setUserDataApi(details) {
    try {
        const response = await api.post('/user/v1/set-user-data', details);
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function updateAboutApi({ about }) {
    try {
        const response = await api.put('/user/v1/user-about', { about });
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function updateBirthDateApi({ birthDate }) {
    try {
        const response = await api.put('/user/v1/user-birth-date', { birthDate });
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function updateUserNameApi({ firstName, lastName }) {
    try {
        const response = await api.put('/user/v1/user-name', { firstName, lastName });
        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}


