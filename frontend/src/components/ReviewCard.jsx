import React from 'react';
import { Link, useNavigate } from "react-router-dom";
import PropTypes from 'prop-types';
import "./style/reviewCard.css"
import {timeAgo} from "../utils/formate.jsx";
import Swal from "sweetalert2";
import {PATHS} from "../constants/constants.jsx";

export default function ReviewCard({
                                       id,
                                       movieId,
                                       avatar,
                                       userId,
                                       name,
                                       date,
                                       rating,
                                       description,
                                       onDelete}) {
    const stars = Array.from({ length: 5 }, (_, i) => i + 1);
    const navigate = useNavigate();

    const goToMovie = () => {
        if (movieId) {
            navigate(PATHS.MOVIE.DETAILS(movieId));
        }
    }

    return (
        <article
            className="review-card"
            aria-label={`Review by ${name}`}
            role="button"
            tabIndex={0}
            onClick={goToMovie}
            onKeyDown={(e) => {
                if ((e.key === 'Enter' || e.key === ' ') && movieId) {
                    e.preventDefault();
                    goToMovie();
                }
            }}
        >
             <header className="review-card-header">
                {/* Profile picture, name and date bundled together on the left of first row*/}
                <div className="review-card-header-left">
                    <div className="review-card-header-left-avatar-wrapper">
                        <Link to={PATHS.USER.PROFILE(userId)} onClick={(e) => e.stopPropagation()}>
                            <img className="review-card-header-left-avatar"
                                 src={avatar}
                                 alt={`${name}'s avatar`}
                                 loading="lazy"
                            />
                        </Link>
                    </div>
                    <div className="review-card-header-left-meta">
                        <div className="review-card-header-left-name">
                            <Link to={PATHS.USER.PROFILE(userId)} onClick={(e) => e.stopPropagation()}>{name}</Link>
                        </div>
                        <div className="review-card-header-left-date">
                            {timeAgo(date)}
                        </div>
                    </div>
                </div>

                {/* Rating (5 starts) and x/10, together at far right*/}
                <div className="review-card-header-right">
                    <div className="review-card-header-right-stars">
                        {stars.map(s => (
                            <span key={s} className={`rc-star ${s <= Math.round((rating/10) * 5) ? 'filled' : ''}`}>★</span>
                        ))}
                    </div>
                    {(rating !== null && rating !== undefined)  &&
                        <div className="review-card-header-right-rating" aria-label={`Rating ${rating} out of 10`}>
                            {rating}/10
                        </div>}
                </div>
            </header>

            <div className="review-card-description">
                <p>{description}</p>
            </div>

            <footer className="review-card-footer">
                {onDelete && (
                    <button
                        className="review-card-delete"
                        onClick={async (e) => {
                            e.stopPropagation();
                            const result = await Swal.fire({
                                title: "Delete Review?",
                                text: "Are you sure you want to delete this review?",
                                icon: "warning",
                                showCancelButton: true,
                                confirmButtonColor: '#d33',
                                cancelButtonColor: '#3085d6',
                                confirmButtonText: "Yes, delete it!",
                                cancelButtonText: "Cancel",
                            });

                            if (result.isConfirmed) onDelete(id);
                        }}
                        aria-label="Delete review"
                    >
                        Delete
                    </button>
                )}
            </footer>

        </article>
    )
}

ReviewCard.propTypes = {
    id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    movieId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    avatar: PropTypes.string,
    userId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    name: PropTypes.string,
    date: PropTypes.string,
    rating: PropTypes.number,
    description: PropTypes.string,
    onDelete: PropTypes.func
    // children: PropTypes.node // have children instead of description
};


ReviewCard.defaultProps = {
    avatar: '',
    name: 'Anonymous',
    date: 'Unknown',
    rating: null,
    description: "No description provided."
};