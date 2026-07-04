import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PropTypes from 'prop-types';
import { Star, Trash2 } from 'lucide-react';
import './style/reviewCard.css';
import { timeAgo } from '../utils/formate.jsx';
import { PATHS } from '../constants/constants.jsx';
import Avatar from './ui/Avatar.jsx';
import ConfirmDialog from './ui/ConfirmDialog.jsx';

export default function ReviewCard({
    id,
    movieId,
    movieTitle,
    avatar,
    userId,
    name = 'Anonymous',
    date = 'Unknown',
    rating = null,
    description = 'No description provided.',
    onDelete,
}) {
    const [confirmOpen, setConfirmOpen] = useState(false);
    const stars = Array.from({ length: 5 }, (_, i) => i + 1);
    const navigate = useNavigate();

    const goToMovie = () => {
        if (movieId) navigate(PATHS.MOVIE.DETAILS(movieId));
    };

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
                <div className="review-card-header-left">
                    <Link to={PATHS.USER.PROFILE(userId)} onClick={(e) => e.stopPropagation()}>
                        <Avatar name={name} src={avatar} size="md" />
                    </Link>
                    <div className="review-card-header-left-meta">
                        <div className="review-card-header-left-name">
                            <Link to={PATHS.USER.PROFILE(userId)} onClick={(e) => e.stopPropagation()}>{name}</Link>
                        </div>
                        <div className="review-card-header-left-date">{timeAgo(date)}</div>
                    </div>
                </div>

                <div className="review-card-header-right">
                    {movieTitle && (
                        <button
                            className="movie-link-right"
                            onClick={(e) => { e.stopPropagation(); navigate(PATHS.MOVIE.DETAILS(movieId)); }}
                            aria-label={`Go to movie ${movieTitle}`}
                        >
                            {movieTitle}
                        </button>
                    )}

                    <div className="review-card-header-right-row">
                        <div className="review-card-header-right-stars">
                            {stars.map((s) => (
                                <Star key={s} size={15} className={s <= Math.round((rating / 10) * 5) ? 'rc-star filled' : 'rc-star'} />
                            ))}
                        </div>
                        {rating != null && (
                            <div className="review-card-header-right-rating" aria-label={`Rating ${rating} out of 10`}>{rating}/10</div>
                        )}
                    </div>
                </div>
            </header>

            <div className="review-card-description">
                <p>{description}</p>
            </div>

            {onDelete && (
                <footer className="review-card-footer">
                    <button
                        className="review-card-delete"
                        onClick={(e) => { e.stopPropagation(); setConfirmOpen(true); }}
                        aria-label="Delete review"
                    >
                        <Trash2 size={14} /> Delete
                    </button>
                </footer>
            )}

            <ConfirmDialog
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={() => { setConfirmOpen(false); onDelete?.(id); }}
                title="Delete review?"
                message="This can't be undone."
                confirmLabel="Delete"
                danger
            />
        </article>
    );
}

ReviewCard.propTypes = {
    id: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    movieId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    movieTitle: PropTypes.string,
    avatar: PropTypes.string,
    userId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    name: PropTypes.string,
    date: PropTypes.string,
    rating: PropTypes.number,
    description: PropTypes.string,
    onDelete: PropTypes.func,
};
