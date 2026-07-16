import api from './api-client.js';


export async function updatePostApi({postId, forumId, title, content}) {
    try{
        const response = await api.put(`/post/v1/${postId}`, {forumId, title, content});
        return { success: true, data: response.data};
    }
    catch(err){
        return { success: false , message: err.message };
    }
}

export async function deletePostApi({postId}) {
    if (!postId) {
        return { success: false, message: 'Invalid postId' };
    }
    try{
        const response = await api.delete(`/post/v1/${postId}`);
        return { success: true, data: response.data};
    }
    catch(err){
        return { success: false , message: err.message };
    }
}

// Post voting goes through the generic vote-api.js (createVoteApi/updateVoteApi/
// deleteVoteApi/isVotedApi) — this module used to duplicate those calls; see VoteWidget.jsx.

export async function getPostApi({ postId }) {
    try {
        const response = await api.get(`/post/v1/${postId}`);
        const data = response.data;

        return { success: true, data: data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function addPostApi({ forumId, title, content }) {
    try {
        const response = await api.post(`/post/v1`, { forumId, title, content });

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function getForumNameApi({ forumId }){
    try {
        const response = await api.get(`/forum/v1/${forumId}/name`);

        return { success: true, data: response.data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function getMainFeedPostsApi({ page, size}) {
    try {
        const res = await api.post(`/post/v1/main-feed`, { page, pageSize:size });
        return { success: true, data: res.data.content };

    }catch (err){
        return { success: false , message: err.message };
    }
}

export async function getExploreFeedPostsApi({ page, size, sort = "hot"}) {
    try {
        // "top"/"score", "new", "hot"
        const res = await api.get('/feed/v1/explore', {params: { page, size, sort }});
        return { success: true, data: res.data.posts };

    }catch (err){
        return { success: false , message: err.message };
    }
}

export async function getMyPostsApi({ page = 0, size = 20 }) {
    try {
        const res = await api.get('/post/v1/my-posts', { params: { page, size } });
        const data = res.data;
        return { success: true, data: data.content || data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function getOtherUserPostsApi({ userId, page = 0, size = 20 }) {
    if (!userId) return { success: false, message: 'Invalid userId' };
    try {
        const res = await api.get(`/post/v1/user/${userId}`, { params: { page, size } });
        const data = res.data;
        return { success: true, data: data.content || data };
    } catch (err) {
        return { success: false, message: err?.message || 'Unknown error', status: err?.status };
    }
}
