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

export function mapBackendPostToFrontend(p) {
    if (!p) return {};

    return {
        postId: p.id,
        forumId: p.forumId,
        title: p.title,
        text: p.content,
        media: p.mediaUrl,
        votes: p.score,
        upvotes: p.upvoteCount,
        downvotes: p.downvoteCount,
        commentCount: p.commentCount,
        createdAt: p.createdAt,
        time: p.createdAt ? new Date(p.createdAt).toLocaleString() : null,
        userId: p.ownerId,
        firstName: p.ownerName?.firstName || 'Anonymous',
        lastName: p.ownerName?.lastName || '',
        avatar: p.ownerAvatar
    }
}

export function mapBackendForumToFrontend(f) {
    if (!f) return {};

    return {
        id: f.id,
        name: f.name,
        description: f.description,
        ownerId: f.ownerId,
        memberCount: f.followerCount,
        postCount: f.postCount,
        createdAt: f.createdAt,
        time: f.createdAt ? new Date(f.createdAt).toLocaleString() : null,
        avatar: f.avatar
    };
}
