import api from './api-client.js';

/**
 * Add a new comment to a post
 * @param {Object} params - Comment parameters
 * @param {string} params.postId - Post ID (ObjectId as string)
 * @param {string} params.parentId - Parent comment ID (ObjectId as string) for replies, null for top-level comments
 * @param {string} params.content - Comment content
 * @returns {Promise<Object>} - Returns success status and comment ID
 */
export async function addCommentApi({ postId, parentId, content }) {
    try {
        const response = await api.post("/comment/v1", {
            postId,
            parentId: parentId || null,
            content
        });

        return { success: true, data: response.data };
    } catch (err) {
        console.error("[API] addComment error:", err);
        return { 
            success: false, 
            message: err.response?.data?.error || err.message 
        };
    }
}

/**
 * Get all comments for a post with pagination
 * @param {Object} params - Query parameters
 * @param {string} params.postId - Post ID (ObjectId as string)
 * @param {number} params.page - Page number (default: 0)
 * @param {number} params.size - Page size (default: 20)
 * @param {string} params.sortBy - Sort method ('score', 'new') (default: 'score')
 * @returns {Promise<Object>} - Returns paginated comments
 */
export async function getPostCommentsApi({ postId, page = 0, size = 20, sortBy = 'score' }) {
    try {
        const response = await api.get(`/comment/v1/post/${postId}`, {
            params: { page, size, sortBy }
        });

        return { success: true, data: response.data };
    } catch (err) {
        console.error("Error fetching comments:", err);
        return { 
            success: false, 
            message: err.response?.data?.error || err.message 
        };
    }
}

/**
 * Get replies for a parent comment
 * @param {Object} params
 * @param {string} params.parentId - Parent comment ID
 * @param {string} params.sortBy - Sort method ('score' or 'new')
 */
export async function getRepliesApi({ parentId, sortBy = 'score' }) {
    try {
        const response = await api.get(`/comment/v1/${parentId}/replies`, {
            params: { sortBy }
        });

        return { success: true, data: response.data };
    } catch (err) {
        console.error('Error fetching replies:', err);
        return { success: false, message: err.response?.data?.error || err.message };
    }
}

/**
 * Delete a comment
 * @param {Object} params - Delete parameters
 * @param {string} params.commentId - Comment ID (ObjectId as string)
 * @returns {Promise<Object>} - Returns success status
 */
export async function deleteCommentApi({ commentId }) {
    try {
        const response = await api.delete(`/comment/v1/${commentId}`);

        return { success: true, data: response.data };
    } catch (err) {
        console.error("Error deleting comment:", err);
        return { 
            success: false, 
            message: err.response?.data?.error || err.message 
        };
    }
}

// Note: comment update handled locally in frontend workflow; no backend endpoint.
// Comment voting goes through the generic vote-api.js (createVoteApi/updateVoteApi/
// deleteVoteApi/isVotedApi) — this module used to duplicate those calls; see VoteWidget.jsx.
