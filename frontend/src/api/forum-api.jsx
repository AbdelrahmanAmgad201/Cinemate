import api from "./api-client.jsx";

export async function createForumApi({ name, description }) {
    try {
        const response = await api.post(`/forum/v1/create`, { name, description });
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function deleteForumApi({ forumId }) {
    try {
        const response = await api.delete(`/forum/v1/delete/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}


export async function updateForumApi({ forumId, name, description }) {
    try {
        const response = await api.put(`/forum/v1/update/${forumId}`, {
            name,
            description
        });
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}


// TODO: This is jus
//  t a guess
export async function getForumApi({ forumId }) {
    try {
        const response = await api.get(`/forum/v1/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}


export async function unfollowForumApi({ forumId }) {
    try {
        const response = await api.delete(`/forum-follow/v1/follow/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}


export async function followForumApi({ forumId }) {
    try {
        const response = await api.put(`/forum-follow/v1/follow/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}


//TODO: I want a check follow endpoint
export async function checkFollowApi({ forumId }) {
    try {
        const response = await api.get(`/forum-follow/v1/check-follow/${forumId}`);
        console.log(response.data);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}


// TODO: Get followed forums
export async function getFollowedForumsApi() {}

// TODO: Get a forum's posts

// TODO: Get some forums based on some criteria

// TODO: Get a forum's mod