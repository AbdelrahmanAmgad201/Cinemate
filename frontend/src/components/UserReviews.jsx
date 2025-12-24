import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import ReviewCard from './ReviewCard.jsx';
import { getUserReviewsApi } from '../api/movie-api.jsx';
import '../pages/user/style/UserProfile.css';

export default function UserReviews({ userId, profile }) {
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        setReviews([]);
        setPage(0);
    }, [userId]);

    useEffect(() => {
        let ignore = false;
        setLoading(true);
        getUserReviewsApi({ userId: Number(userId), page, size: 10 })
            .then(res => {
                if (ignore) return;
                if (res?.success && res.data) {
                    const content = res.data.content || [];
                    setReviews(prev => (page > 0 ? [...prev, ...content] : content));
                    setTotalPages(res.data.totalPages ?? 0);
                }
            })
            .catch(e => {
                console.error(e);
            })
            .finally(() => { if (!ignore) setLoading(false); });

        return () => { ignore = true; };
    }, [userId, page]);

    return (
        <div>
            {loading ? (
                <p>Loading reviews...</p>
            ) : (reviews.length === 0) ? (
                <p className="placeholder-note">No reviews by this user.</p>
            ) : (
                <>
                    <div className="user-reviews-list">
                        {reviews.map(r => (
                            <ReviewCard
                                key={r.id}
                                id={r.id}
                                userId={r.reviewerId ?? Number(userId)}
                                name={r.name}
                                date={r.date}
                                rating={r.rating}
                                description={r.description}
                                avatar={profile?.avatar || ''}
                            />
                        ))}
                    </div>
                    {totalPages > page + 1 && (
                        <div style={{ marginTop: 12 }}>
                            <button className="btn btn-outline" onClick={() => setPage(p => p + 1)}>Load more</button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}

UserReviews.propTypes = {
    userId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    profile: PropTypes.object
};
