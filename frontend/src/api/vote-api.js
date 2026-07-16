import api from './api-client.js';

export async function createVoteApi({ targetId, value, isPost = true }) {
    try {
        const path = isPost ? "/vote/v1/post" : "/vote/v1/comment";
        const response = await api.post(path, { targetId, value });
        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error creating vote:', err);
        return { success: false, status: err.response?.status, message: err.response?.data?.message || err.message };
    }
}

export async function updateVoteApi({ targetId, value }) {
    try {
        const response = await api.put('/vote/v1', { targetId, value });
        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error updating vote:', err);
        return { success: false, status: err.response?.status, message: err.response?.data?.message || err.message };
    }
}

export async function deleteVoteApi({ targetId }) {
    try {
        const response = await api.delete(`/vote/v1/${targetId}`);
        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error deleting vote:', err);
        return { success: false, status: err.response?.status, message: err.response?.data?.message || err.message };
    }
}

export async function isVotedApi({ targetId }) {
    try {
        const response = await api.get(`/vote/v1/${targetId}`);
        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error checking vote:', err);
        return { success: false, status: err.response?.status, message: err.response?.data?.message || err.message };
    }
}
