import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Swiper, SwiperSlide } from 'swiper/react';

import 'swiper/css';
import 'swiper/css/pagination';
import 'swiper/css/navigation';
import './style/carousel.css';
import { Star, Play } from 'lucide-react';
import { Autoplay, Pagination, Navigation } from 'swiper/modules';
import MoviesDetailsApi from '../api/movies-details-api.js';
import { PATHS } from '../constants/constants.jsx';
import Skeleton from './ui/Skeleton.jsx';

const HERO_SLIDE_COUNT = 5;

/**
 * Hero carousel on the Browse page — pulls real top-rated movies from the
 * backend instead of a fixed set of local poster images, so it reflects
 * whatever is actually in the catalog (including "nothing yet").
 */
export default function Carousel() {
    const [movies, setMovies] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        let mounted = true;
        MoviesDetailsApi({
            name: null, genre: null, sortBy: 'rating', sortDirection: 'desc', page: 0, pageSize: HERO_SLIDE_COUNT,
        }).then((res) => {
            if (!mounted) return;
            if (res.success) {
                setMovies(res.movies.map((movie) => ({
                    id: movie.movieID,
                    title: movie.name,
                    poster: movie.thumbnailUrl,
                    duration: movie.duration,
                    rating: movie.averageRating,
                })));
            }
            setLoading(false);
        });
        return () => { mounted = false; };
    }, []);

    if (loading) {
        return <div className="carousel"><Skeleton variant="rect" className="carousel__skeleton" /></div>;
    }

    if (movies.length === 0) return null;

    return (
        <div className="carousel">
            <Swiper
                spaceBetween={30}
                centeredSlides
                autoplay={{ delay: 5000, disableOnInteraction: false }}
                pagination={{ clickable: true }}
                navigation
                modules={[Autoplay, Pagination, Navigation]}
                loop
                allowTouchMove={false}
                className="mySwiper"
            >
                {movies.map((movie) => (
                    <SwiperSlide key={movie.id}>
                        <div className="img-card">
                            <div className="img-container">
                                <img src={movie.poster} alt={movie.title} />
                            </div>
                            <div className="overlay" />
                            <div className="info">
                                <h2 className="movie-title">{movie.title}</h2>
                                <div className="movie-details">
                                    <button className="play-button" onClick={() => navigate(PATHS.MOVIE.DETAILS(movie.id))}>
                                        <Play size={16} fill="currentColor" /> View details
                                    </button>
                                    {movie.duration && (
                                        <span className="duration">{Math.floor(movie.duration / 60)}h {movie.duration % 60}min</span>
                                    )}
                                    {movie.rating != null && (
                                        <span className="rating"><Star size={16} fill="#ffc107" color="#ffc107" /> {movie.rating}/10</span>
                                    )}
                                </div>
                            </div>
                        </div>
                    </SwiperSlide>
                ))}
            </Swiper>
        </div>
    );
}
