import api from './api-client.jsx';

export async function createVote({ targetId, value, isPost = true }) {
    try {
        const path = isPost ? "/vote/v1/post-vote" : "/vote/v1/comment-vote";
        const response = await api.post(path, { targetId, value });
        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error creating vote:', err);
        return { success: false, status: err.response?.status, message: err.response?.data?.message || err.message };
    }
}

export async function updateVote({ targetId, value }) {
    try {
        const response = await api.put('/vote/v1/update-vote', { targetId, value });
        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error updating vote:', err);
        return { success: false, status: err.response?.status, message: err.response?.data?.message || err.message };
    }
}

export async function deleteVote({ targetId }) {
    try {
        const response = await api.delete(`/vote/v1/delete-vote/${targetId}`);
        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error deleting vote:', err);
        return { success: false, status: err.response?.status, message: err.response?.data?.message || err.message };
    }
}

export async function isVoted({ targetId }) {
    try {
        const response = await api.get(`/vote/v1/is-voted/${targetId}`);
        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error checking vote:', err);
        return { success: false, status: err.response?.status, message: err.response?.data?.message || err.message };
    }
}
