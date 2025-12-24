import React, { useEffect, useState, useContext } from 'react';
import MoviesList from './MoviesList.jsx';
import { getWatchLaterApi, getMovieApi } from '../api/movie-api.jsx';
import { useNavigate } from 'react-router-dom';
import { ToastContext } from '../context/ToastContext.jsx';
import { MdNavigateNext, MdNavigateBefore } from 'react-icons/md';

function WatchLaterPanel({ pageSize = 8 }) {
    const navigate = useNavigate();
    const { showToast } = useContext(ToastContext);

    const [list, setList] = useState([]);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        let ignore = false;
        setLoading(true);

        getWatchLaterApi({ page, size: pageSize })
            .then(res => {
                if (ignore) return;
                if (res?.success && res.data) {
                    const data = res.data;
                    const content = (data.content || []).map(it => ({ id: it.id ?? it.watchLaterID_MovieId, title: it.movieName || it.movieName }));
                    setTotalPages(data.totalPages ?? 0);

                    Promise.all(content.map(c => getMovieApi({ movieId: c.id }).then(r => r.success ? r.data : null)))
                        .then(details => {
                            if (ignore) return;
                            const merged = content.map((c, idx) => ({ ...c, ...(details[idx] || {}) }));
                            setList(merged);
                        })
                        .catch(() => {
                            if (!ignore) setList(content);
                        });
                } else {
                    showToast('Failed to load watch later', res?.message || 'Unknown error', 'error');
                }
            }).catch(() => {
                if (!ignore) showToast('Failed to load watch later', 'Unknown error', 'error');
            }).finally(() => { if (!ignore) setLoading(false); });

        return () => { ignore = true; };
    }, [page, pageSize, showToast]);

    const canPrev = page > 0;
    const canNext = (typeof totalPages === 'number' && totalPages > 0) ? page < (totalPages - 1) : (list.length === pageSize);

    const handleClickMovie = React.useCallback((movieId) => navigate(`/movie/${movieId}`), [navigate]);
    const goPrev = React.useCallback(() => setPage(p => Math.max(0, p - 1)), []);
    const goNext = React.useCallback(() => setPage(p => p + 1), []);

    return (
        <div className="watchlater-inner" style={{ position: 'relative' }}>
            <MoviesList
                list={list}
                name="Watch Later"
                page={-1}
                onClick={handleClickMovie}
            />

            {loading && (
                <div className="watchlater-loading-overlay" aria-hidden>
                    <div className="watchlater-spinner" />
                </div>
            )}

            <div className="watchlater-pager">
                <button className={`watchlater-arrow ${!canPrev ? 'disabled' : ''}`} onClick={goPrev} aria-label="Previous page" disabled={!canPrev || loading}>
                    <MdNavigateBefore />
                </button>

                <div className="watchlater-page-indicator">{page + 1}{totalPages ? ` / ${totalPages}` : ''}</div>

                <button className={`watchlater-arrow ${!canNext ? 'disabled' : ''}`} onClick={goNext} aria-label="Next page" disabled={!canNext || loading}>
                    <MdNavigateNext />
                </button>
            </div>
        </div>
    );
}

export default React.memo(WatchLaterPanel);

