import { useEffect, useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import Carousel from '../../components/Carousel.jsx';
import MoviesList from '../../components/MoviesList.jsx';
import Footer from '../../components/Footer.jsx';
import MoviesDetailsApi from '../../api/movies-details-api.js';
import { PATHS } from '../../constants/constants.jsx';
import { GENRES } from '../../constants/genres.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import GenreTile from '../../components/ui/GenreTile.jsx';
import './style/Browse.css';

const PAGE_SIZE = 6;

export default function Browse() {
    const [newReleases, setNewReleases] = useState([]);
    const [topRated, setTopRated] = useState([]);
    const [loading, setLoading] = useState(true);

    const [newReleasesPage, setNewReleasesPage] = useState(0);
    const [topRatedPage, setTopRatedPage] = useState(0);
    const [newReleasesTotalPages, setNewReleasesTotalPages] = useState(0);
    const [topRatedTotalPages, setTopRatedTotalPages] = useState(0);

    const navigate = useNavigate();
    const { showToast } = useContext(ToastContext);

    useEffect(() => {
        let mounted = true;

        async function fetchMovies() {
            setLoading(true);
            try {
                const [newReleasesResponse, topRatedResponse] = await Promise.all([
                    MoviesDetailsApi({ name: null, genre: null, sortBy: 'releaseDate', sortDirection: 'desc', page: newReleasesPage, pageSize: PAGE_SIZE }),
                    MoviesDetailsApi({ name: null, genre: null, sortBy: 'rating', sortDirection: 'desc', page: topRatedPage, pageSize: PAGE_SIZE }),
                ]);
                if (!mounted) return;

                if (newReleasesResponse.success) {
                    setNewReleases(newReleasesResponse.movies.map((movie) => ({
                        id: movie.movieID, title: movie.name, poster: movie.thumbnailUrl, duration: movie.duration, rating: movie.averageRating,
                    })));
                    setNewReleasesTotalPages(newReleasesResponse.totalPages || 0);
                } else {
                    showToast('Failed to load new releases', newReleasesResponse.message, 'error');
                }

                if (topRatedResponse.success) {
                    setTopRated(topRatedResponse.movies.map((movie) => ({
                        id: movie.movieID, title: movie.name, poster: movie.thumbnailUrl, duration: movie.duration, rating: movie.averageRating,
                    })));
                    setTopRatedTotalPages(topRatedResponse.totalPages || 0);
                } else {
                    showToast('Failed to load top rated movies', topRatedResponse.message, 'error');
                }
            } finally {
                if (mounted) setLoading(false);
            }
        }

        fetchMovies();
        return () => { mounted = false; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [newReleasesPage, topRatedPage]);

    return (
        <div className="browse-page">
            <main className="browse-page__main">
                <Carousel />

                <div className="browse-page__rows">
                    <MoviesList
                        list={newReleases}
                        name="New Releases"
                        loading={loading}
                        page={newReleasesPage}
                        setPage={setNewReleasesPage}
                        totalPages={newReleasesTotalPages}
                        onClick={(id) => navigate(PATHS.MOVIE.DETAILS(id))}
                    />
                    <MoviesList
                        list={topRated}
                        name="Top Rated"
                        loading={loading}
                        page={topRatedPage}
                        setPage={setTopRatedPage}
                        totalPages={topRatedTotalPages}
                        onClick={(id) => navigate(PATHS.MOVIE.DETAILS(id))}
                    />

                    <section className="movies-list">
                        <h2 className="movies-list__title">Browse by genre</h2>
                        <div className="browse-page__genre-grid">
                            {GENRES.map((genre) => (
                                <GenreTile
                                    key={genre.key}
                                    label={genre.label}
                                    color={genre.color}
                                    onClick={() => navigate(PATHS.MOVIE.GENRE(genre.key))}
                                />
                            ))}
                        </div>
                    </section>
                </div>
            </main>
            <Footer />
        </div>
    );
}
