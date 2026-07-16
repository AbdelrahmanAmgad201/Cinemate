import { useState, useEffect, useContext, useRef } from 'react';
import { AuthContext } from '../../context/AuthContext.jsx';
import './style/MoviePreviewPage.css';
import ReviewCard from '../../components/ReviewCard.jsx';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { Play, Film, Bookmark, Heart, Users, Star, Clock, Plus, MessageSquareText } from 'lucide-react';

import { getMovieApi, getReviewsApi, postReviewApi, deleteReviewApi, likeMovieApi, unlikeMovieApi, addToWatchHistoryApi, addToWatchLaterApi, removeFromWatchLaterApi, getIsLikedApi, getIsWatchLaterApi } from '../../api/movie-api.js';
import { ToastContext } from '../../context/ToastContext.jsx';

import { MAX_LENGTHS, PATHS, ROLES } from '../../constants/constants.jsx';
import { WatchPartyContext } from '../../context/WatchPartyContext.jsx';
import Button from '../../components/ui/Button.jsx';
import IconButton from '../../components/ui/IconButton.jsx';
import Badge from '../../components/ui/Badge.jsx';
import Modal from '../../components/ui/Modal.jsx';
import Textarea from '../../components/ui/Textarea.jsx';
import Input from '../../components/ui/Input.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import LoadingFallback from '../../components/LoadingFallback.jsx';

export default function MoviePreviewPage() {
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);
    const { createParty } = useContext(WatchPartyContext);

    const { movieId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    const [movie, setMovie] = useState(location.state?.movie ?? null);
    const [movieLoading, setMovieLoading] = useState(false);
    const [isLiked, setIsLiked] = useState(false);
    const [isWatchLater, setIsWatchLater] = useState(false);

    const [reviews, setReviews] = useState([]);
    const [reviewsLoading, setReviewsLoading] = useState(false);
    const [reviewsError, setReviewsError] = useState(null);

    const [page, setPage] = useState(0);
    const [size] = useState(10);
    const [totalPages, setTotalPages] = useState(1);

    const sentinelRef = useRef(null);
    const fetchingRef = useRef(false);

    const [showForm, setShowForm] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [formRating, setFormRating] = useState(5);
    const [formDesc, setFormDesc] = useState('');

    const handleAdd = async (e) => {
        e.preventDefault();
        if (!formDesc.trim()) {
            showToast('Warning', 'Please enter a description.', 'warning');
            return;
        }
        setSubmitting(true);

        const res = await postReviewApi({ movieId, comment: formDesc, rating: Number(formRating) });

        if (res.success) {
            const review = res.data;
            setReviews((prev) => [review, ...prev]);
            setPage(0);
            await fetchReviews(movieId, 0, false);
            await fetchMovie();

            setFormDesc('');
            setFormRating(5);
            setShowForm(false);
        } else {
            showToast('Failed to submit review', res.message || 'unknown error', 'error');
        }

        setSubmitting(false);
    };

    const handleDelete = async (id) => {
        const prev = reviews;
        setReviews((r) => r.filter((x) => x.id !== id));
        const res = await deleteReviewApi({ movieId });
        if (!res.success) {
            setReviews(prev);
            showToast('Failed to delete review', res.message || 'unknown error', 'error');
        } else {
            await fetchMovie();
        }
    };

    const fetchReviews = async (id, pageToFetch = 0, append = false) => {
        if (!id) return;
        setReviewsError(null);
        if (!append) setReviewsLoading(true);
        fetchingRef.current = true;

        const res = await getReviewsApi({ movieId: id, page: pageToFetch, size });
        if (!res.success) {
            if (user.role === ROLES.USER) showToast('Failed to fetch reviews', res.message || 'unknown error', 'error');
            setReviewsError(res.message || 'Failed to load reviews');
            setReviewsLoading(false);
            fetchingRef.current = false;
            return;
        }

        const data = res.data;
        const pageContent = data.content ?? data;
        setTotalPages(data.totalPages ?? Math.max(1, Math.ceil((data.totalElements ?? pageContent.length) / size)));
        setPage(pageToFetch);
        setReviews((prev) => {
            if (!append) return pageContent;
            const ids = new Set(prev.map((r) => r.id));
            const newItems = pageContent.filter((r) => !ids.has(r.id));
            return [...prev, ...newItems];
        });

        setReviewsLoading(false);
        fetchingRef.current = false;
    };

    const fetchMovie = async () => {
        setMovieLoading(true);
        const res = await getMovieApi({ movieId });
        if (!res.success) {
            showToast('Failed to fetch movie', res.message || 'unknown error', 'error');
            setMovieLoading(false);
            return;
        }
        setMovie(res.data);
        setMovieLoading(false);
    };

    const handleLike = async () => {
        const res = isLiked ? await unlikeMovieApi({ movieId }) : await likeMovieApi({ movieId });
        if (!res.success) {
            showToast(isLiked ? 'Failed to unlike movie' : 'Failed to like movie', res.message || 'unknown error', 'error');
            return;
        }
        setIsLiked(!isLiked);
        showToast(isLiked ? 'Removed like' : 'Liked movie', isLiked ? 'Removed movie from like list' : 'Added movie to like list', 'success');
    };

    const handleWatchLater = async () => {
        const res = isWatchLater ? await removeFromWatchLaterApi({ movieId }) : await addToWatchLaterApi({ movieId });
        if (!res.success) {
            showToast(isWatchLater ? 'Failed to remove from watch later' : 'Failed to add movie to watch later', res.message || 'unknown error', 'error');
            return;
        }
        setIsWatchLater(!isWatchLater);
        showToast(isWatchLater ? 'Removed' : 'Watch later movie', isWatchLater ? 'Removed movie from watch later list' : 'Added movie to watch later list', 'success');
    };

    const handleWatchTogether = async () => {
        showToast('Watch together', 'Starting a new watch together room...', 'info');
        const res = await createParty(movieId);
        if (!res.success) {
            showToast('Failed to create a watch together room', res.message || 'unknown error', 'error');
            return;
        }
        showToast('Watch together', 'Room created successfully', 'success');
        navigate(PATHS.MOVIE.WATCH_PARTY(res.data.partyId));
    };

    useEffect(() => {
        if (location.state?.movie) {
            const m = location.state.movie;
            setMovie(m);
            const id = m.id ?? movieId;
            if (id) {
                setReviews([]);
                setPage(0);
                setTotalPages(1);
                fetchReviews(id);
            }
        } else if (movieId) {
            (async () => {
                await fetchMovie();
                setReviews([]);
                setPage(0);
                setTotalPages(1);
                await fetchReviews(movieId, 0, true);
            })();
        } else {
            setMovie(null);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [movieId, location.state?.movie]);

    useEffect(() => {
        if (!movieId || !user || user.role !== ROLES.USER) return;
        let ignore = false;

        getIsLikedApi({ movieId }).then((res) => {
            if (!ignore && res.success) setIsLiked(Boolean(res.data));
        });
        getIsWatchLaterApi({ movieId }).then((res) => {
            if (!ignore && res.success) setIsWatchLater(Boolean(res.data));
        });

        return () => { ignore = true; };
    }, [movieId, user]);

    useEffect(() => {
        if (!sentinelRef.current) return;
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting && !fetchingRef.current && page + 1 < totalPages) {
                    fetchReviews(movie.id, page + 1, true);
                }
            });
        }, { root: null, rootMargin: '200px', threshold: 0.1 });

        observer.observe(sentinelRef.current);
        return () => observer.disconnect();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [sentinelRef.current, page, totalPages, movieId]);

    if (movieLoading) return <LoadingFallback fullScreen />;

    if (!movie) {
        return (
            <EmptyState
                icon={<Film size={28} />}
                title="Movie not found"
                description="This movie may have been removed or the link is incorrect."
                actionLabel="Back to browse"
                onAction={() => navigate(PATHS.MOVIE.BROWSE)}
            />
        );
    }

    return (
        <div className="movie-preview-page">
            <div className="movie-preview-page-movie">
                <div className="movie-preview-page-movie-poster">
                    {movie.poster
                        ? <img src={movie.poster} alt={`${movie.title} poster`} />
                        : <div className="movie-preview-page-movie-poster-fallback"><Film size={40} /></div>}
                </div>

                <div className="movie-preview-page-movie-details-middle">
                    <div className="movie-preview-page-movie-details-middle-title">
                        <h1>{movie.title}</h1>
                    </div>

                    {(movie.genres || []).length > 0 && (
                        <div className="movie-preview-page-movie-details-middle-genres">
                            {movie.genres.map((g) => <Badge key={g} variant="neutral">{g}</Badge>)}
                        </div>
                    )}

                    <div className="movie-preview-page-movie-details-middle-description">
                        <p>{movie.description}</p>
                    </div>

                    <div className="movie-preview-page-movie-details-middle-buttons">
                        <Button
                            icon={<Play size={18} fill="currentColor" />}
                            onClick={() => {
                                addToWatchHistoryApi({ movieId });
                                navigate(PATHS.MOVIE.WATCH, { state: movie.videoUrl });
                            }}
                        >
                            Play
                        </Button>

                        <Button variant="secondary" icon={<Film size={18} />} onClick={() => navigate(PATHS.MOVIE.WATCH, { state: movie.trailerUrl })}>
                            Trailer
                        </Button>

                        {user.role === ROLES.USER && (
                            <>
                                <IconButton label={isWatchLater ? 'Remove from watchlist' : 'Add to watchlist'} variant="solid" size="lg" active={isWatchLater} onClick={handleWatchLater}>
                                    <Bookmark size={18} fill={isWatchLater ? 'currentColor' : 'none'} />
                                </IconButton>
                                <IconButton label={isLiked ? 'Unlike' : 'Like'} variant="solid" size="lg" active={isLiked} onClick={handleLike}>
                                    <Heart size={18} fill={isLiked ? 'currentColor' : 'none'} />
                                </IconButton>
                                <IconButton label="Start watch party" variant="solid" size="lg" onClick={handleWatchTogether}>
                                    <Users size={18} />
                                </IconButton>
                            </>
                        )}
                    </div>
                </div>

                <div className="movie-preview-page-movie-details-right">
                    <div className="movie-preview-page-movie-details-right-top">
                        <span className="rating"><Star size={16} fill="currentColor" />{movie.rating ?? '—'}/10</span>
                        {movie.runtime && <span className="runtime"><Clock size={16} />{movie.runtime}</span>}
                    </div>

                    {movie.releaseDate && (
                        <div className="movie-preview-page-movie-details-right-credits">
                            <dl>
                                <dt>Release date</dt>
                                <dd>{movie.releaseDate}</dd>
                            </dl>
                        </div>
                    )}
                </div>
            </div>

            {user.role === ROLES.USER && (
                <div className="movie-preview-page-reviews">
                    <div className="movie-preview-page-reviews-header-row">
                        <h2 className="reviews-title">User reviews ({reviews.length})</h2>
                        <Button size="sm" icon={<Plus size={16} />} onClick={() => setShowForm(true)}>Add review</Button>
                    </div>

                    <div className="movie-preview-page-reviews-list">
                        {reviewsLoading && (
                            <div className="movie-preview-page-reviews-skeletons">
                                {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} variant="rect" height={140} />)}
                            </div>
                        )}
                        {!reviewsLoading && reviewsError && (
                            <p className="reviews-error">Couldn't load reviews: {reviewsError}</p>
                        )}
                        {!reviewsLoading && !reviewsError && reviews.length === 0 && (
                            <EmptyState
                                icon={<MessageSquareText size={28} />}
                                title="No reviews yet"
                                description="Be the first to share what you thought."
                            />
                        )}
                        {reviews.map((r) => (
                            <ReviewCard
                                key={r.id ?? r.userId}
                                id={r.id ?? r.userId}
                                movieId={r.movieId}
                                avatar={r.avatar}
                                userId={r.reviewerId ?? r.userId}
                                name={r.name}
                                date={r.date}
                                rating={r.rating}
                                description={r.description}
                                onDelete={r.reviewerId === user?.id ? handleDelete : undefined}
                            />
                        ))}
                        <div ref={sentinelRef} style={{ height: 1 }} aria-hidden="true" />
                    </div>

                    <Modal
                        open={showForm}
                        onClose={() => setShowForm(false)}
                        title="Add a review"
                        footer={
                            <>
                                <Button variant="ghost" onClick={() => setShowForm(false)} disabled={submitting}>Cancel</Button>
                                <Button onClick={handleAdd} loading={submitting}>Add review</Button>
                            </>
                        }
                    >
                        <form className="add-review-form" onSubmit={handleAdd}>
                            <Input
                                label="Rating (0-10)"
                                type="number"
                                min="0"
                                max="10"
                                step="1"
                                value={formRating}
                                onChange={(e) => setFormRating(Number(e.target.value))}
                            />
                            <Textarea
                                label="Description"
                                rows={5}
                                value={formDesc}
                                onChange={(e) => setFormDesc(e.target.value.slice(0, MAX_LENGTHS.TEXTAREA))}
                                placeholder={`Write your review (max ${MAX_LENGTHS.TEXTAREA} characters)`}
                                maxLength={MAX_LENGTHS.TEXTAREA}
                                required
                            />
                        </form>
                    </Modal>
                </div>
            )}
        </div>
    );
}
