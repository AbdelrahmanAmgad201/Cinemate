export function mapBackendReviews(data) {

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

import { formatRuntime } from "./formate.jsx";
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
