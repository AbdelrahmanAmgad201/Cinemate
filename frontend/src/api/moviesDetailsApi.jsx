import api from './apiClient.jsx';

export default async function MoviesDetailsApi( request) {
    try {
        const response = await api.post("/movie/v1/search", request );
        const data = response.data;

        return { success: true, movies: data.content };
    } catch (err) {
        return { success: false , message: err.message };
    }
}
