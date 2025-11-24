import {useState, useEffect, useContext} from 'react'
import { AuthContext } from "../../context/AuthContext.jsx";
import './style/moviePreviewPage.css'
import ReviewCard from "../../components/reviewCard.jsx";
import { dummyReviews } from "../../data/dummyReviews";


import playIcon from "../../assets/icons/play-black.png";
import trailerIcon from "../../assets/icons/film-black.png";
import watchlistIcon from "../../assets/icons/book-mark-black.png";
import likeIcon from "../../assets/icons/like-black.png";
import watchPartyIcon from "../../assets/icons/watch-party-black.png";
import addIcon from "./../../assets/icons/add-white.png"
import clockIcon from "../../assets/icons/clock-white.png";

export default function MoviePreviewPage() {

    // Movie details



    // Reviews and modal
    const [reviews, setReviews] = useState([]);
    const [showForm, setShowForm] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [formRating, setFormRating] = useState(5);
    const [formDesc, setFormDesc] = useState("");

    const { user, loading, signIn, signOut, isAuthenticated } = useContext(AuthContext);


    // add a new review
    const handleAdd = async (e) => {
        e.preventDefault();
        if (!formDesc.trim()) {
            alert("Please enter a description.");
            return;
        }
        setSubmitting(true);

        // TODO: send to backend
        const newReview = {
            userId: user.id,
            // avatar: formAvatar,
            // date: new Date().toISOString(),
            rating: Number(formRating),
            description: formDesc
        };

        // optimistic UI update
        // setReviews(prev => [newReview, ...prev]);

        setFormDesc("");
        setFormRating(5);
        setShowForm(false);
        setSubmitting(false);

        await getReviews();

    };

    // GUI-only delete (optimistic)
    const handleDelete = async (id) => {
        // const prev = reviews;
        setReviews(r => r.filter(x => x.id !== id));
        // TODO: send to backend
    };

    const getReviews = async () => {
        // TODO: send to backend

        setReviews(dummyReviews);
    }

    // fetch reviews when component mounts
    useEffect(() => {
        const fetchReviews = async () => {
            await getReviews();
        };

        fetchReviews();
    }, [])

    const getMovieDetails = async () => {
        // TODO: send to backend


    }
    return (
        <div className="movie-preview-page">
            {/* Vertical (Columns) elements*/}
            <div className="movie-preview-page-movie">
                <div className="movie-preview-page-movie-poster">
                    <img src="" alt="X Poster" title='X Poster'/>
                </div>

                {/* Horizontal (Rows) elements*/}
                <div className="movie-preview-page-movie-details-middle">
                    <div className="movie-preview-page-movie-details-middle-title">
                        <h1>Movie Title</h1>
                    </div>

                    <div className="movie-preview-page-movie-details-middle-genres">
                        <span className="genre-tag">Science Fiction</span>
                        <span className="genre-tag">Comedy</span>
                    </div>

                    <div className="movie-preview-page-movie-details-middle-description">
                        <p>Movie Desc</p>
                    </div>

                    <div className="movie-preview-page-movie-details-middle-buttons">
                        <button title="Play">
                            <img src={playIcon} alt="Play" className="button-icon" />
                            Play
                        </button>

                        <button title="Watch Trailer">
                            <img src={trailerIcon} alt="Trailer" className="button-icon" />
                        </button>

                        <button title="Watchlist">
                            <img src={watchlistIcon} alt="Watchlist" className="button-icon" />
                        </button>

                        <button title="Like">
                            <img src={likeIcon} alt="Like" className="button-icon" />
                        </button>

                        <button title="Watch-Party">
                            <img src={watchPartyIcon} alt="Watch-Party" className="button-icon" />
                        </button>

                    </div>
                </div>

                <div className="movie-preview-page-movie-details-right">
                    <div className="movie-preview-page-movie-details-right-top">
                        <span className="rating">
                            <span className="rating-star-icon">★</span>
                            7.9/10
                        </span>
                        <span className="runtime">
                            <img src={clockIcon} alt="clockIcon" className="clock-icon" />
                            2h 12m
                        </span>
                    </div>

                    <div className="movie-preview-page-movie-details-right-credits">
                        <dl>
                            <dt>Directors</dt>
                            <dd>James Gunn</dd>
                            <dt>Writers</dt>
                            <dd>James Gunn, Others</dd>
                            <dt>Cast</dt>
                            <dd>James Gunn, Others</dd>
                            <dt>Producers</dt>
                            <dd>James Gunn, Others</dd>
                            <dt>Studio</dt>
                            <dd>Marvel Studio</dd>
                        </dl>
                    </div>
                </div>
            </div>

            {/* Vertical (Columns) elements*/}
            <div className="movie-preview-page-reviews">
                <div className="movie-preview-page-reviews-header-row">
                    <h2 className="reviews-title">User Reviews (748)</h2>
                    <button className="reviews-add-button" title="Add Review" onClick={() => setShowForm(true)}>
                        <img src={addIcon} alt="Add Review" className="button-icon" />
                    </button>
                </div>

                <div className="movie-preview-page-reviews-list">
                    {dummyReviews.map((r, index) => (
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
                    ))}
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
                                        value={formRating}
                                        onChange={e => setFormRating(e.target.value)}
                                    />
                                </label>

                                <label>
                                    Description
                                    <textarea rows="5" value={formDesc} onChange={e => setFormDesc(e.target.value)} />
                                </label>

                                <div className="modal-actions">
                                    <button type="button" onClick={() => setShowForm(false)}>Cancel</button>
                                    <button type="submit" disabled={submitting}>{submitting ? "Adding..." : "Add Review"}</button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

            </div>

        </div>
    )


}