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