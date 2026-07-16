import api from './api-client.js';

export async function getExploreForumsApi({ page, size, sort: sort = "new"}) {
    try {
        // new
        // "followers" -
        // "posts"
        const res = await api.get('/feed/v1/explore-forums', { params: { page, size , sort}});
        const data = res.data;

        return { success: true, data: data.forums };
    } catch (err) {
        return { success: false, message: err.message };
    }
}
