import {useState, useEffect, useContext, useRef} from 'react'
import { AuthContext } from "../../context/AuthContext.jsx";
import './style/moviePreviewPage.css'
import ReviewCard from "../../components/reviewCard.jsx";
import { dummyReviews } from "../../data/dummyReviews";
import {useParams, useLocation, useNavigate} from "react-router-dom";

import {getMovieApi, getReviewsApi, postReviewApi, deleteReviewApi, likeMovieApi, addToWatchHistoryApi, addToWatchLaterApi} from "../../api/movie-api.jsx";
import {ToastContext} from "../../context/ToastContext.jsx";

import playIcon from "../../assets/icons/play-black.png";
import trailerIcon from "../../assets/icons/film-black.png";
import watchlistIcon from "../../assets/icons/book-mark-black.png";
import likeIcon from "../../assets/icons/like-black.png";
import watchPartyIcon from "../../assets/icons/watch-party-black.png";
import addIcon from "./../../assets/icons/add-white.png"
import clockIcon from "../../assets/icons/clock-white.png";

import {MAX_LENGTHS, PATHS, ROLES} from "../../constants/constants.jsx";

export default function MoviePreviewPage() {

    // Movie details
    const { movieId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    // In home-page, use as follow
    // <Link to={PATHS.MOVIE.DETAILS(movieId)} state={{ movie }}>
    //     <MovieCard/>
    // </Link>
    const [movie, setMovie] = useState(location.state?.movie ?? null);
    const [movieLoading, setMovieLoading] = useState(false);
    const [movieError, setMovieError] = useState(null);


    // Reviews and modal
    const [reviews, setReviews] = useState([]);
    const [reviewsLoading, setReviewsLoading] = useState(false);
    const [reviewsError, setReviewsError] = useState(null);

    const [page, setPage] = useState(0);
    const [size] = useState(10);            // page size
    const [totalPages, setTotalPages] = useState(1);

    const sentinelRef = useRef(null);       // for intersection observer
    const fetchingRef = useRef(false);      // prevent double fetch

    const [showForm, setShowForm] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [formRating, setFormRating] = useState(5);
    const [formDesc, setFormDesc] = useState("");

    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    // add a new review
    const handleAdd = async (e) => {
        e.preventDefault();
        if (!formDesc.trim()) {
            showToast("Warning", "Please enter a description.", "warning")
            return;
        }
        setSubmitting(true);

        // TODO: send to backend
        console.log({movieId, formDesc, formRating})
        const res = await postReviewApi({movieId, comment: formDesc, rating: Number(formRating)});
        const review = res.data
        // res is my review, so put it first it to the list of reviews
        if (res.success === true) {
            const review = res.data;
            setReviews(prev => [review, ...prev]); // show immediately
            setPage(0);
            await fetchReviews(movieId, 0, false); // sync with backend
        }
        else {
            showToast("Failed to submit review", res.message || "unknown error", "error")
        }


        setFormDesc("");
        setFormRating(5);
        setShowForm(false);
        setSubmitting(false);

        await fetchReviews(movieId, page, true);

    };

    /////////////////////////////////////////////////////////// TODO /////////////////////////////////////////////////////
    const handleDelete = async (id) => {

        // if (!window.confirm("Delete this review?")) return;
        const prev = reviews;
        setReviews(r => r.filter(x => x.id !== id));
        // const res = await deleteReviewApi({ reviewId: id });
        const res = await deleteReviewApi({movieId});
        if (res.success === false) {
            setReviews(prev); // rollback
            showToast("Failed to delete review", res.message || "unknown error", "error")
        }
    };

    const fetchReviews = async (movieId, pageToFetch = 0, append = false) => {
        if (!movieId){
            return;
        }
        setReviewsError(null);
        if (!append){
            setReviewsLoading(true);
        }
        fetchingRef.current = true;

        // movieId, page, size

        const res = await getReviewsApi({movieId, page:pageToFetch, size});
        if (res.success === false) {
            {user.role === ROLES.USER && showToast("Failed to fetch reviews", res.message || "unknown error", "error")}
            setReviewsError(res.message || "Failed to load reviews");
            setReviewsLoading(false);
            fetchingRef.current = false;
            return;
        }

        const data = res.data
        console.log(data)
        const pageContent = data.content ?? data;
        setTotalPages(data.totalPages ?? Math.max(1, Math.ceil((data.totalElements ?? pageContent.length) / size)));
        setPage(pageToFetch);
        // adds new page content to existing reviews OR replaces existing reviews if append is false
        setReviews(prev => {
            if (!append) return pageContent;
            // append while avoiding duplicates by id
            const ids = new Set(prev.map(r => r.id));
            const newItems = pageContent.filter(r => !ids.has(r.id));
            return [...prev, ...newItems];
        });

        setReviewsLoading(false);
        fetchingRef.current = false;

    }

    const fetchMovie = async () => {
        setMovieError(null);
        setMovieLoading(true);

        const res = await getMovieApi({ movieId });
        if (!res.success){
            showToast("Failed to fetch movie", res.message || "unknown error", "error")
            setMovieError(res.message || "Failed to load movie");
            setMovieLoading(false);
            return;
        }

        setMovie(res.data);
        setMovieLoading(false);

    }

    const handleLike = async () => {

        const res = await likeMovieApi({ movieId });
        if (res.success === false){
            showToast("Failed to like movie", res.message || "unknown error", "error")
            return
        }

        showToast("Liked movie", "Added movie to like list", "success")

    }

    const handleWatchLater = async () => {
        const res = await addToWatchLaterApi({ movieId });
        if (res.success === false){
            showToast("Failed to add movie to watch later", res.message || "unknown error", "error")
            return
        }

        showToast("Watch later movie", "Added movie to watch later list", "success")
    }

    // Fetches reviews and maybe movie details from backend
    useEffect(() => {
        // If movie details are passed in via location state (clicked on from home page), use them

        if (location.state?.movie) {

            const m = location.state.movie;
            setMovie(m);
            const id = m.id ?? movieId;
            if (id) {
                // reset pagination
                setReviews([]);
                setPage(0);
                setTotalPages(1);
                fetchReviews(id);
            }
        }
        // If someone goes to the movie page directly, fetch movie details from backend
        else if (movieId){
            (async () => {
                await fetchMovie();

                // reset pagination
                setReviews([]);
                setPage(0);
                setTotalPages(1);
                await fetchReviews(movieId, 0, true);
            })();
        }
        else {
            // THIS SHOULD NOT HAPPEN
            setMovie(null);
            console.error("Movie ID not found in location state or params.");
        }
    }, [movieId, location.state?.movie]);

    // the following is for infinite scrolling on reviews, i do not understand it
    useEffect(() => {
        if (!sentinelRef.current) return;
        const observer = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (entry.isIntersecting && !fetchingRef.current && page + 1 < totalPages) {
                    fetchReviews(movie.id, page + 1, true);
                }
            });
        }, {
            root: null,
            rootMargin: '200px',
            threshold: 0.1
        });

        observer.observe(sentinelRef.current);
        return () => observer.disconnect();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [sentinelRef.current, page, totalPages, movieId]);


    // COMMENT TO TEST
    if (movieLoading) return <div className="loader">Loading movie...</div>;
    // if (movieError) return <div className="error">Error: {movieError}</div>;
    if (!movie) return <div className="no-movie">No movie selected.</div>;

    return (
        <div className="movie-preview-page">
            {/* Vertical (Columns) elements*/}
            <div className="movie-preview-page-movie">
                <div className="movie-preview-page-movie-poster">
                    <img src={movie.poster || ""} alt= {`${movie.title} Poster`} title='X Poster'/>
                </div>

                {/* Horizontal (Rows) elements*/}
                <div className="movie-preview-page-movie-details-middle">
                    <div className="movie-preview-page-movie-details-middle-title">
                        <h1>{movie.title}</h1>
                    </div>

                    <div className="movie-preview-page-movie-details-middle-genres">
                        {(movie.genres || []).map((g, index) => <span key={g} className="genre-tag">{g}</span>)}
                    </div>

                    <div className="movie-preview-page-movie-details-middle-description">
                        <p>{movie.description}</p>
                    </div>

                    <div className="movie-preview-page-movie-details-middle-buttons">
                        <button title="Play" onClick={() => {
                            addToWatchHistoryApi({movieId});
                            navigate(PATHS.MOVIE.WATCH, { state:  movie.videoUrl  })
                        }}>
                            <img src={playIcon} alt="Play" className="button-icon" />
                            Play
                            <span className="tooltip">Play movie</span>
                        </button>

                        <button title="Watch Trailer" onClick={() => navigate(PATHS.MOVIE.WATCH, { state: movie.trailerUrl })}>
                            <img src={trailerIcon} alt="Trailer" className="button-icon" />
                            <span className="tooltip">Watch Trailer</span>
                        </button>

                        {user.role === ROLES.USER && <button title="Watchlist">
                            <img src={watchlistIcon} alt="Watchlist" className="button-icon" onClick={handleWatchLater}/>
                            <span className="tooltip">Watchlist</span>
                        </button>}

                        {user.role === ROLES.USER && <button title="Like">
                            <img src={likeIcon} alt="Like" className="button-icon" onClick={handleLike} />
                            <span className="tooltip">Like</span>
                        </button>}

                        {user.role === ROLES.USER && <button title="Watch-Party">
                            <img src={watchPartyIcon} alt="Watch-Party" className="button-icon" />
                            <span className="tooltip">Start Watch-Party</span>
                        </button>}

                    </div>
                </div>

                <div className="movie-preview-page-movie-details-right">
                    <div className="movie-preview-page-movie-details-right-top">
                        <span className="rating">
                            <span className="rating-star-icon">★</span>
                            {movie.rating ?? "—"}/10
                        </span>
                        <span className="runtime">
                            <img src={clockIcon} alt="clockIcon" className="clock-icon" />
                            {movie.runtime}
                        </span>
                    </div>

                    <div className="movie-preview-page-movie-details-right-credits">
                        <dl>
                            <dt>Release Date</dt>
                            <dd>{movie.releaseDate}</dd>
                        </dl>
                    </div>
                </div>
            </div>

            {/* Vertical (Columns) elements*/}
            {user.role === ROLES.USER && <div className="movie-preview-page-reviews">
                <div className="movie-preview-page-reviews-header-row">
                    <h2 className="reviews-title">User Reviews ({reviews.length})</h2>
                    <button className="reviews-add-button" title="Add Review" onClick={() => setShowForm(true)}>
                        <img src={addIcon} alt="Add Review" className="button-icon" />
                        <span className="tooltip">Add review</span>
                    </button>
                </div>

                <div className="movie-preview-page-reviews-list">
                    {reviewsLoading && (
                        <div className="loader">Loading reviews...</div>)}
                    {reviewsError && (
                        <div className="error">Error: {reviewsError}</div>)}
                    {reviews.length === 0 && (
                        <div className="no-reviews">No reviews yet. Be the first!</div>)}
                    {(reviews.map((r, index) => (
                        <ReviewCard
                            key={r.userId}
                            avatar={r.avatar}
                            userId={r.userId}
                            name={r.name}
                            date={r.date}
                            rating={r.rating}
                            description={r.description}
                            onDelete={r.userId === user?.id ? handleDelete : undefined} // only allow delete for own review
                        />
                    )))}
                    {/* sentinel for infinite scroll */}
                    <div ref={sentinelRef} style={{ height: 1 }} aria-hidden="true" />
                </div>

                {showForm && (
                    <div className="modal-overlay" onMouseDown={() => setShowForm(false)}>
                        <div className="modal" onMouseDown={(e) => e.stopPropagation()}>
                            <h3>Add Review</h3>
                            <form onSubmit={handleAdd}>
                                <label>
                                    Rating (0-10)
                                    <input
                                        type="number"
                                        min="0"
                                        max="10"
                                        step="1"
                                        value={formRating}
                                        onChange={e => setFormRating(Number(e.target.value))}
                                    />
                                </label>

                                <label>
                                    Description
                                    <textarea
                                        rows="5"
                                        value={formDesc} onChange={e => {
                                            const inputValue = e.target.value;
                                            if (inputValue.length <= MAX_LENGTHS.TEXTAREA) {
                                                setFormDesc(inputValue);
                                            }
                                        }}
                                        placeholder={`Write your review (max ${MAX_LENGTHS.TEXTAREA} words)`}
                                        maxLength={MAX_LENGTHS.TEXTAREA}
                                        required
                                    />
                                    <small>{formDesc.trim().split(/\s+/).length} / 500 words</small>
                                </label>

                                <div className="modal-actions">
                                    <button type="button" onClick={() => setShowForm(false)}>Cancel</button>
                                    <button type="submit" disabled={submitting}>{submitting ? "Adding..." : "Add Review"}</button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

            </div>}

        </div>
    )


}