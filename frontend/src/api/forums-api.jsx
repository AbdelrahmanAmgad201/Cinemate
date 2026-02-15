import api from './api-client.jsx';
import { mapBackendForumToFrontend } from '../utils/api-mappers.jsx';
import mockForums from '../data/followed-forums.jsx';

export async function getFollowedForumsApi({ page, size }) {
    try {
        const res = await api.get(`/forum-follow/v1/followed`, { params: { page, size } });
        const data = res.data;
        // console.log(data);

        return { success: true, data: data };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function getUserForumsApi({ page, size }) {
    try {
        const res = await api.get(`/forum/v1/user-forums`, { params: { page, size } });
        const data = res.data || {};
        const content = Array.isArray(data.content) ? data.content.map(mapBackendForumToFrontend) : [];
        const out = { ...data, content };
        return { success: true, data: out };
    } catch (err) {
        return { success: false, message: err.message };
    }
}