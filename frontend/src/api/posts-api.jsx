import api from './api-client.jsx';
import { mapBackendPostToFrontend } from '../utils/api-mappers.jsx';
import { getMockPosts } from '../data/mock-posts.jsx';
import mockPosts from '../data/mock-posts.jsx';

export async function getMainFeedPostsApi({ page, size}) {
    try {
        const res = await api.post(`/post/v1/user-main-feed`, { page, pageSize:size });
        const data = res.data;
        // console.log(data);

        return { success: true, data: data.content };

    }catch (err){
        return { success: false , message: err.message };
    }
}

export async function getExploreFeedPostsApi({ page, size, sort = "hot"}) {
    try {
        // "top"/"score", "new", "hot"
        const res = await api.get('/feed/explore', {params: { page, size, sort }});
        const data = res.data;
        console.log(data);

        return { success: true, data: data.posts };

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
        const res = await api.get(`/post/v1/other-user-posts/${userId}`, { params: { page, size } });
        const data = res.data;
        return { success: true, data: data.content || data };
    } catch (err) {
        return { success: false, message: err?.message || 'Unknown error', status: err?.status };
    }
}