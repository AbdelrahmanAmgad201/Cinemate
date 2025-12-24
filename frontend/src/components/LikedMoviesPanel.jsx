import React, { useEffect, useState, useCallback } from 'react';
import MoviesList from './MoviesList.jsx';
import { getOtherUserLikedMoviesApi, getMyLikedMoviesApi, getMovieApi } from '../api/movie-api.jsx';
import { MdNavigateNext, MdNavigateBefore } from 'react-icons/md';
import { useNavigate } from 'react-router-dom';

function LikedMoviesPanel({ userId, my = false, pageSize = 8 }) {
    const navigate = useNavigate();
    const [list, setList] = useState([]);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        let ignore = false;
        setLoading(true);
        const loader = my ? getMyLikedMoviesApi.bind(null, { page, size: pageSize }) : getOtherUserLikedMoviesApi.bind(null, { userId, page, size: pageSize });
        Promise.resolve()
            .then(() => loader())
            .then(res => {
                if (ignore) return;
                if (res?.success && res.data) {
                    const data = res.data;
                    const content = (data.content || []).map(it => ({ id: it.id, title: it.title }));
                    setTotalPages(data.totalPages ?? 0);
                    Promise.all(content.map(c => getMovieApi({ movieId: c.id }).then(r => r.success ? r.data : null)))
                        .then(details => { if (ignore) return; const merged = content.map((c, idx) => ({ ...c, ...(details[idx] || {}) })); setList(merged); })
                        .catch(() => { if (!ignore) setList(content); });
                } else {
                    setList([]);
                }
            }).catch(() => { if (!ignore) setList([]); })
            .finally(() => { if (!ignore) setLoading(false); });
        return () => { ignore = true; };
    }, [userId, page, pageSize, my]);

    const canPrev = page > 0;
    const canNext = (typeof totalPages === 'number' && totalPages > 0) ? page < (totalPages - 1) : (list.length === pageSize);

    const handleClickMovie = useCallback((movieId) => navigate(`/movie/${movieId}`), [navigate]);
    const goPrev = useCallback(() => setPage(p => Math.max(0, p - 1)), []);
    const goNext = useCallback(() => setPage(p => p + 1), []);

    return (
        <div className="liked-inner" style={{ position: 'relative' }}>
            <MoviesList list={list} name="Liked Movies" page={-1} onClick={handleClickMovie} />

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

export default React.memo(LikedMoviesPanel);
