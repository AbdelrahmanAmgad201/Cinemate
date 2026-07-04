import { useState } from 'react';
import PropTypes from 'prop-types';
import { Star, Play, Film } from 'lucide-react';
import Skeleton from './Skeleton.jsx';
import './style/MovieCard.css';

function formatDuration(minutes) {
    if (!minutes) return null;
    return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}

/**
 * Single poster card used everywhere a movie is listed (browse grid, genre
 * grid, carousels, watch history/later/liked panels) — replaces the
 * duplicated <img>+title+meta markup that used to live in each of those
 * components separately.
 */
export default function MovieCard({ movie, onClick, size = 'md' }) {
    const [imgFailed, setImgFailed] = useState(false);
    const duration = formatDuration(movie.duration);

    return (
        <button type="button" className={`movie-card movie-card--${size}`} onClick={onClick}>
            <span className="movie-card__poster">
                {movie.poster && !imgFailed ? (
                    <img
                        src={movie.poster}
                        alt={movie.title}
                        loading="lazy"
                        onError={() => setImgFailed(true)}
                    />
                ) : (
                    <span className="movie-card__poster-fallback">
                        <Film size={32} aria-hidden="true" />
                    </span>
                )}
                <span className="movie-card__overlay">
                    <span className="movie-card__play"><Play size={20} fill="currentColor" /></span>
                </span>
            </span>
            <span className="movie-card__title">{movie.title}</span>
            {(duration || movie.rating != null) && (
                <span className="movie-card__meta">
                    {duration && <span>{duration}</span>}
                    {movie.rating != null && (
                        <span className="movie-card__rating">
                            <Star size={13} fill="currentColor" /> {movie.rating}/10
                        </span>
                    )}
                </span>
            )}
        </button>
    );
}

MovieCard.propTypes = {
    movie: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
        title: PropTypes.string.isRequired,
        poster: PropTypes.string,
        duration: PropTypes.number,
        rating: PropTypes.number,
    }).isRequired,
    onClick: PropTypes.func,
    size: PropTypes.oneOf(['sm', 'md', 'lg']),
};

export function MovieCardSkeleton({ size = 'md' }) {
    return (
        <div className={`movie-card movie-card--${size}`}>
            <Skeleton variant="rect" className="movie-card__poster" />
            <Skeleton variant="text" width="80%" style={{ marginTop: 10 }} />
            <Skeleton variant="text" width="50%" style={{ marginTop: 6 }} />
        </div>
    );
}

MovieCardSkeleton.propTypes = {
    size: PropTypes.oneOf(['sm', 'md', 'lg']),
};
