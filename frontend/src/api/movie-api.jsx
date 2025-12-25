import api from './api-client.jsx';
import { mapBackendReviews, mapMovieBackendToFrontend } from "../utils/api-mappers.jsx";

export async function getMovieApi({ movieId }) {
    try {
        const response = await api.post(`/movie/v1/get-specific-movie/${movieId}`);
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
        const response = await api.get(`/movie-review/v1/get-movie-reviews/${movieId}`, {
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

export async function postReviewApi({movieId, comment, rating}) {
    // private Long movieId;
    // private String comment;
    // private Integer rating;
    try{
        const response = await api.post("/movie-review/v1/add-review", {movieId, comment, rating});

        const data = response.data;
        const userReview = mapBackendReviews({ content: [data] })[0];

        console.log(userReview);

        return { success: true, data: userReview};
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.message };
    }
};

export async function deleteReviewApi({movieId}) {
    // private Long movieId;
    try{
        const response = await api.delete(`/delete-movie-review/${movieId}`);

        const data = response.data;


        return { success: true,};
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.message };
    }
};



// Like and watch later
export async function likeMovieApi({movieId}) {
    // private Long movieId;
    try{
        const response = await api.post(`/liked-movie/v1/like-movie/${movieId}`);
        console.log(response);
        const data = response.data;


        return { success: true,};
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.message };
    }
};

export async function addToWatchLaterApi({movieId}) {
    // private Long movieId;
    try{
        const response = await api.post(`/watch-later/v1/watch-later/${movieId}`);
        console.log(response);
        const data = response.data;


        return { success: true,};
    }
    catch(err){
        console.log(err);
        return { success: false , message: err.message };
    }
};

export async function addToWatchHistoryApi({movieId}) {
    // private Long movieId;
    try{
        const response = await api.post(`/watch-history/v1/add-watch-history/${movieId}`);
        console.log(response);
        const data = response.data;


        return { success: true,};
    }
    catch(err){
        // console.log(err);
        return { success: false , message: err.message };
    }
};

export async function getOtherUserLikedMoviesApi({ userId, page = 0, size = 8 } = {}) {
    try {
        const response = await api.get(`/liked-movie/v1/other-user-liked-movies/${userId}`, { params: { page, size } });
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
        const response = await api.get(`/watch-history/v1/watch-history`, { params: { page, size } });
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
        const response = await api.get(`/watch-later/v1/watch-later`, { params: { page, size } });
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