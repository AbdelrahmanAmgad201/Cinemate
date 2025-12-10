import api from './api-client.jsx';
import { mapBackendForumToFrontend } from '../utils/api-mappers.jsx';
import mockForums from '../data/followed-forums.jsx';

export async function getFollowedForumsApi({ page = 0, size = 10 } = {}) {
    try {
        const res = await api.get(`/forum/v1/followed`, { params: { page, size } });
        const data = res.data;
        if (Array.isArray(data)) {
            return { success: true, data: data.map(mapBackendForumToFrontend) };
        }
        const content = data?.content || [];
        return {
            success: true,
            data: content.map(mapBackendForumToFrontend),
            page: data?.page ?? page,
            totalPages: data?.totalPages
        }
    } catch (err) {
        const start = page * size;
        const end = start + size;
        const content = mockForums.slice(start, end);
        return { success: true, data: content, page, totalPages: Math.ceil(mockForums.length / size) };
    }
}

export async function getForumByIdApi({ forumId }) {
    try {
        const res = await api.get(`/forum/v1/get/${forumId}`);
        return { success: true, data: mapBackendForumToFrontend(res.data) };
    } catch (err) {
        return { success: false, message: err.message };
    }
}
