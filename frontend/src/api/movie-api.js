import api from './api-client.js';
import { mapBackendReviews, mapMovieBackendToFrontend } from "../utils/api-mappers.jsx";

export async function getMovieApi({ movieId }) {
    try {
        const response = await api.get(`/movie/v1/${movieId}`);
        const data = response.data;

        const mappedMovie = mapMovieBackendToFrontend(data);
        return { success: true, data: mappedMovie };
    } catch (err) {
        return { success: false, message: err.message };
    }
}

export async function getReviewsApi({movieId, page, size}) {
    try{
        // movieId;
        // pageable
        const response = await api.get(`/movie-review/v1/movie/${movieId}`, {
            params: { page, size }})

        const data = response.data;
        return {
            success: true,
            data: {
                ...data,
                content: mapBackendReviews(data) // map only the content, keep totalPages etc
            }
        };

    }catch(err){
        return { success: false , message: err.message };
    }
};

export async function getUserReviewsApi({userId, page, size}) {
    try{
        const response = await api.get(`/movie-review/v1/user/${userId}`, {
            params: { page, size }})

        const data = response.data;
        return {
            success: true,
            data: {
                ...data,
                content: mapBackendReviews(data)
            }
        };

    }catch(err){
        return { success: false , message: err.message };
    }
};

export async function getMyReviewsApi({page, size}) {
    try{
        const response = await api.get(`/movie-review/v1/my-reviews`, {
            params: { page, size }
        });
        const data = response.data;
        return {
            success: true,
            data: {
                ...data,
                content: mapBackendReviews(data)
            }
        };
    }catch(err){
        return { success: false, message: err.message };
    }
};

export async function postReviewApi({movieId, comment, rating}) {
    // private Long movieId;
    // private String comment;
    // private Integer rating;
    try{
        const response = await api.post("/movie-review/v1", {movieId, comment, rating});

        const data = response.data;
        const userReview = mapBackendReviews({ content: [data] })[0];

        return { success: true, data: userReview};
    }
    catch(err){
        console.error('Error posting review:', err);
        return { success: false , message: err.message };
    }
};

export async function deleteReviewApi({movieId}) {
    try{
        await api.delete(`/movie-review/v1/${movieId}`);

        return { success: true,};
    }
    catch(err){
        console.error('Error deleting review:', err);
        return { success: false , message: err.message };
    }
};



// Like and watch later
export async function likeMovieApi({movieId}) {
    try{
        await api.put(`/liked-movie/v1/${movieId}`);
        return { success: true,};
    }
    catch(err){
        return { success: false , message: err.message };
    }
};

export async function addToWatchLaterApi({movieId}) {
    try{
        await api.put(`/watch-later/v1/${movieId}`);
        return { success: true,};
    }
    catch(err){
        return { success: false , message: err.message };
    }
};

export async function unlikeMovieApi({movieId}) {
    try{
        await api.delete(`/liked-movie/v1/${movieId}`);
        return { success: true,};
    }
    catch(err){
        return { success: false , message: err.message };
    }
};

export async function removeFromWatchLaterApi({movieId}) {
    try{
        await api.delete(`/watch-later/v1/${movieId}`);
        return { success: true,};
    }
    catch(err){
        return { success: false , message: err.message };
    }
};

export async function getIsLikedApi({movieId}) {
    try{
        const response = await api.get(`/liked-movie/v1/${movieId}`);
        return { success: true, data: response.data };
    }
    catch(err){
        return { success: false , message: err.message };
    }
};

export async function getIsWatchLaterApi({movieId}) {
    try{
        const response = await api.get(`/watch-later/v1/${movieId}`);
        return { success: true, data: response.data };
    }
    catch(err){
        return { success: false , message: err.message };
    }
};

export async function addToWatchHistoryApi({movieId}) {
    try{
        await api.post(`/watch-history/v1/${movieId}`);
        return { success: true,};
    }
    catch(err){
        return { success: false , message: err.message };
    }
};

export async function getOtherUserLikedMoviesApi({ userId, page = 0, size = 8 } = {}) {
    try {
        const response = await api.get(`/liked-movie/v1/user/${userId}`, { params: { page, size } });
        const data = response.data;
        const mapped = {
            ...data,
            content: (data.content || []).map(item => ({ id: item.likedMoviesID_MovieId, title: item.movieName }))
        };
        return { success: true, data: mapped };
    } catch (err) {
        return { success: false, message: err.message };
    }
};

export async function getMyLikedMoviesApi({ page = 0, size = 8 } = {}) {
    try {
        const response = await api.get(`/liked-movie/v1/my-liked-movies`, { params: { page, size } });
        const data = response.data;
        const mapped = {
            ...data,
            content: (data.content || []).map(item => ({ id: item.likedMoviesID_MovieId, title: item.movieName }))
        };
        return { success: true, data: mapped };
    } catch (err) {
        return { success: false, message: err.message };
    }
};

export async function getWatchHistoryApi({page = 0, size = 20} = {}) {
    try {
        const response = await api.get(`/watch-history/v1`, { params: { page, size } });
        const data = response.data;
        const content = Array.isArray(data.content) ? data.content.map(item => ({
            ...item,
            watchedAt: item.watchedAt ? new Date(item.watchedAt) : null
        })) : [];
        return { success: true, data: { ...data, content } };
    } catch (err) {
        return { success: false, message: err.message };
    }
};

export async function getWatchLaterApi({ page = 0, size = 20 } = {}) {
    try {
        const response = await api.get(`/watch-later/v1`, { params: { page, size } });
        const data = response.data;
        const mapped = {
            ...data,
            content: (data.content || []).map(item => ({
                id: item?.watchLaterID?.movieId ?? item?.watchLaterID_MovieId ?? item?.id ?? (item.movie?.movieID ?? item.movieId),
                movieName: item.movieName ?? item.movie?.name ?? ''
            }))
        };
        return { success: true, data: mapped };
    } catch (err) {
        return { success: false, message: err.message };
    }
};