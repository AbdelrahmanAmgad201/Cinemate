import api from './apiClient.jsx';

function mapBackendReviews(data) {

    if (!data?.content) return [];

    return data.content.map(item => ({
        id: item.movieReviewID?.reviewerId ?? Math.random(), // fallback ID if needed
        reviewerId: item.movieReviewID?.reviewerId,
        rating: item.rating,
        description: item.comment,
        date: item.createdAt,
        name: item.reviewer
            ? `${item.reviewer.firstName ?? ""} ${item.reviewer.lastName ?? ""}`.trim() || "Anonymous"
            : "Anonymous"
    }));
}

function formatRuntime(minutes) {
    if (!minutes) return "";
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return `${h}h ${m}m`;
}


export function mapMovieBackendToFrontend(m) {
    return {
        id: m.movieID,
        title: m.name,
        description: m.description,
        poster: m.thumbnailUrl,       // rename to poster for UI
        videoUrl: m.movieUrl,
        trailerUrl: m.trailerUrl,
        genres: [m.genre],            // backend gives ONE genre — UI expects array
        rating: m.averageRating,
        runtime: formatRuntime(m.duration),
        releaseDate: m.releaseDate
    };
}

export async function getMovieApi({ movieId }) {
    try {
        const response = await api.post(`/movie/v1/get-specific-movie/${movieId}`);
        const data = response.data;
        console.log(data);

        const mappedMovie = mapMovieBackendToFrontend(data);
        return { success: true, data: mappedMovie };
    } catch (err) {
        console.error(err);
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
        return { success: false , message: err.response?.data?.error || err.message };
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
        return { success: false , message: err.response?.data?.error || err.message };
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
        return { success: false , message: err.response?.data?.error || err.message };
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
        return { success: false , message: err.response?.data?.error || err.message };
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
        return { success: false , message: err.response?.data?.error || err.message };
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
        console.log(err);
        return { success: false , message: err.response?.data?.error || err.message };
    }
};