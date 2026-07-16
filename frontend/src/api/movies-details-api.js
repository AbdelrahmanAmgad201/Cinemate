import api from './api-client.js';

export default async function moviesDetailsApi(request) {
    try {
        const response = await api.post('/movie/v1/search', request);
        const data = response.data;

        return {
            success: true,
            movies: data.content,
            totalPages: data.totalPages,
            totalElements: data.totalElements,
        };
    } catch (err) {
        return { success: false, message: err.message };
    }
}
