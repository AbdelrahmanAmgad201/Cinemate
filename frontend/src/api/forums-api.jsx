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

export async function followForumApi({ forumId }) {
    try {
        await api.post('/forum/v1/follow', null, { params: { forumId } });
        return { success: true };
    } catch (err) {
        try {
            const key = 'mock_followed_forums';
            const raw = localStorage.getItem(key);
            const arr = raw ? JSON.parse(raw) : [];
            if (!arr.includes(forumId)) arr.push(forumId);
            localStorage.setItem(key, JSON.stringify(arr));
        } catch (e) {
            // ignore localStorage errors
        }
        return { success: true, mock: true };
    }
}

export async function unfollowForumApi({ forumId }) {
    try {
        await api.post('/forum/v1/unfollow', null, { params: { forumId } });
        return { success: true };
    } catch (err) {
        try {
            const key = 'mock_followed_forums';
            const raw = localStorage.getItem(key);
            const arr = raw ? JSON.parse(raw) : [];
            const idx = arr.indexOf(forumId);
            if (idx !== -1) arr.splice(idx, 1);
            localStorage.setItem(key, JSON.stringify(arr));
        } catch (e) {
            // ignore localStorage errors
        }
        return { success: true, mock: true };
    }
}
