import { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MoviesList from '../../components/MoviesList.jsx';
import Footer from '../../components/Footer.jsx';
import MoviesDetailsApi from '../../api/movies-details-api.js';
import { PATHS } from '../../constants/constants.jsx';
import { getGenreLabel } from '../../constants/genres.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';

const PAGE_SIZE = 18;

const Genre = () => {
    const [loading, setLoading] = useState(true);
    const [movies, setMovies] = useState([]);
    const [totalPages, setTotalPages] = useState(0);
    const [currPage, setCurrPage] = useState(0);
    const { title } = useParams();
    const navigate = useNavigate();
    const { showToast } = useContext(ToastContext);

    useEffect(() => {
        let mounted = true;

        async function fetchMovies() {
            setLoading(true);
            const moviesResponse = await MoviesDetailsApi({
                name: null, genre: title.toUpperCase(), sortBy: null, sortDirection: null, page: currPage, pageSize: PAGE_SIZE,
            });
            if (!mounted) return;

            if (moviesResponse.success) {
                setMovies(moviesResponse.movies.map((movie) => ({
                    id: movie.movieID, title: movie.name, poster: movie.thumbnailUrl, duration: movie.duration, rating: movie.averageRating,
                })));
                setTotalPages(moviesResponse.totalPages || 0);
            } else {
                showToast('Failed to load movies', moviesResponse.message, 'error');
            }
            setLoading(false);
        }

        fetchMovies();
        return () => { mounted = false; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [title, currPage]);

    return (
        <div className="genre-page">
            <main>
                <MoviesList
                    list={movies}
                    name={getGenreLabel(title)}
                    loading={loading}
                    page={currPage}
                    setPage={setCurrPage}
                    totalPages={totalPages}
                    onClick={(id) => navigate(PATHS.MOVIE.DETAILS(id))}
                />
            </main>
            <Footer />
        </div>
    );
};

export default Genre;
