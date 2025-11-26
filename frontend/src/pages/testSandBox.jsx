import React, { useEffect, useState } from 'react';
import { getReviewsApi, postReviewApi } from "../api/movieApi.jsx";

export default function TestSandbox() {
    const [reviews, setReviews] = useState([]);
    const [newReview, setNewReview] = useState(null);
    const movieId = 6; // example movieId

    // Fetch reviews on mount
    useEffect(() => {
        const fetchReviews = async () => {
            const res = await getReviewsApi({ movieId, page: 0, size: 10 });
            console.log("Fetched reviews:", res);
            if (res.success) setReviews(res.data.content);
        };

        fetchReviews();
    }, []);

    // Example: add a review
    const handleAddReview = async () => {
        const res = await postReviewApi({
            movieId,
            comment: "This is a test review",
            rating: 9
        });
        console.log("Added review:", res);
        if (res.success) setNewReview(res.data);
    };

    return (
        <div>
            <h1>Test Sandbox</h1>
            <button onClick={handleAddReview}>Add Test Review</button>
            <h2>Reviews:</h2>
            <ul>
                {reviews.map(r => (
                    <li key={r.id}>
                        {r.name} ({r.rating}): {r.description}
                    </li>
                ))}
            </ul>
            {newReview && (
                <div>
                    <h3>New Review Added:</h3>
                    <p>{newReview.name} ({newReview.rating}): {newReview.description}</p>
                </div>
            )}
        </div>
    );
}
