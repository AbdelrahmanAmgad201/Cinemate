import api from './api-client.jsx';

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
        const response = await api.post("/comment/v1/create-comment", {
            postId,
            parentId: parentId || null,
            content
        });

        return { success: true, data: response.data };
    } catch (err) {
        console.error("Error adding comment:", err);
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
        const response = await api.get(`/comment/v1/posts/${postId}/comments`, {
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
        const response = await api.get(`/comment/v1/replies/${parentId}`, {
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
        const response = await api.delete(`/comment/v1/delete-comment/${commentId}`);

        return { success: true, data: response.data };
    } catch (err) {
        console.error("Error deleting comment:", err);
        return { 
            success: false, 
            message: err.response?.data?.error || err.message 
        };
    }
}

/**
 * Update a comment
 * @param {Object} params
 * @param {string} params.commentId
 * @param {string} params.content
 */
// Note: comment update handled locally in frontend workflow; no backend endpoint.

/**
 * Vote on a comment
 * @param {Object} params - Vote parameters
 * @param {string} params.commentId - Comment ID (ObjectId as string)
 * @param {number} params.value - Vote value (1 for upvote, -1 for downvote)
 * @returns {Promise<Object>} - Returns success status
 */
export async function voteCommentApi({ commentId, value }) {
    try {
        const response = await api.post("/vote/v1/comment-vote", {
            targetId: commentId,
            value: value
        });

        return { success: true, data: response.data };
    } catch (err) {
        console.error("Error voting on comment:", err);
        return { 
            success: false, 
            message: err.response?.data?.error || err.message 
        };
    }
}

/**
 * Update vote on a comment
 * @param {Object} params - Vote parameters
 * @param {string} params.commentId - Comment ID (ObjectId as string)
 * @param {number} params.value - New vote value (1 for upvote, -1 for downvote)
 * @returns {Promise<Object>} - Returns success status
 */
export async function updateVoteCommentApi({ commentId, value }) {
    try {
        const response = await api.put("/vote/v1/update-vote", {
            targetId: commentId,
            value: value
        });

        return { success: true, data: response.data };
    } catch (err) {
        console.error("Error updating vote on comment:", err);
        return { 
            success: false, 
            message: err.response?.data?.error || err.message 
        };
    }
}

/**
 * Delete vote on a comment
 * @param {Object} params - Vote parameters
 * @param {string} params.commentId - Comment ID (ObjectId as string)
 * @returns {Promise<Object>} - Returns success status
 */
export async function deleteVoteCommentApi({ commentId }) {
    try {
        const response = await api.delete(`/vote/v1/delete-vote/${commentId}`);

        return { success: true, data: response.data };
    } catch (err) {
        console.error("Error deleting vote on comment:", err);
        return { 
            success: false, 
            message: err.response?.data?.error || err.message 
        };
    }
}

/**
 * Check if user has voted on a comment
 * @param {Object} params - Vote parameters
 * @param {string} params.commentId - Comment ID (ObjectId as string)
 * @returns {Promise<Object>} - Returns vote value (1, -1, or 0)
 */
export async function isVotedCommentApi({ commentId }) {
    try {
        const response = await api.get(`/vote/v1/is-voted/${commentId}`);

        return { success: true, data: response.data };
    } catch (err) {
        console.error("Error checking comment vote:", err);
        return { 
            success: false, 
            message: err.response?.data?.error || err.message 
        };
    }
}
