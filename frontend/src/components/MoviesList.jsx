import { memo } from 'react';
import PropTypes from 'prop-types';
import { Film } from 'lucide-react';
import './style/moviesList.css';
import MovieCard, { MovieCardSkeleton } from './ui/MovieCard.jsx';
import Pagination from './ui/Pagination.jsx';
import EmptyState from './ui/EmptyState.jsx';

/**
 * Generic movie grid + optional pagination, shared by Browse, Genre, and the
 * profile watch-later/liked-movies panels. Pass `page={-1}` (no `setPage`)
 * when the caller renders its own external pager instead.
 */
function MoviesList({ list, name, page, setPage, totalPages, onClick, loading = false }) {
    const showPagination = setPage && typeof page === 'number' && page >= 0 && totalPages > 1;

    return (
        <section className="movies-list">
            {name && <h2 className="movies-list__title">{name}</h2>}

            {loading && (
                <div className="movies-list__grid">
                    {Array.from({ length: 6 }).map((_, i) => <MovieCardSkeleton key={i} />)}
                </div>
            )}

            {!loading && list.length === 0 && (
                <EmptyState
                    icon={<Film size={28} />}
                    title="Nothing here yet"
                    description="Check back once more movies have been added."
                />
            )}

            {!loading && list.length > 0 && (
                <div className="movies-list__grid">
                    {list.map((movie, index) => (
                        <MovieCard
                            key={movie.id ?? movie.title ?? index}
                            movie={movie}
                            onClick={() => onClick && onClick(movie.id != null ? movie.id : movie.title)}
                        />
                    ))}
                </div>
            )}

            {showPagination && (
                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            )}
        </section>
    );
}

MoviesList.propTypes = {
    list: PropTypes.array.isRequired,
    name: PropTypes.string,
    page: PropTypes.number,
    setPage: PropTypes.func,
    totalPages: PropTypes.number,
    onClick: PropTypes.func,
    loading: PropTypes.bool,
};

export default memo(MoviesList);
