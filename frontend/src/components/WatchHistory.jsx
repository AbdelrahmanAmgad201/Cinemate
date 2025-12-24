import { useEffect, useState, useContext } from 'react';
import { Link } from 'react-router-dom';
import { ToastContext } from '../context/ToastContext.jsx';
import { getWatchHistoryApi, getMovieApi } from '../api/movie-api.jsx';
import { PATHS } from '../constants/constants.jsx';
import './style/watchHistory.css';

export default function WatchHistory({ active, isOwnProfile }) {
    const { showToast } = useContext(ToastContext);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [historyItems, setHistoryItems] = useState([]);
    const [historyPage, setHistoryPage] = useState(0);
    const [historyTotalPages, setHistoryTotalPages] = useState(0);
    const [movieMap, setMovieMap] = useState({});

    const fetchPosters = async (movieIds) => {
        const idsToFetch = Array.from(new Set(movieIds)).filter(id => id && !movieMap[id]);
        if (idsToFetch.length === 0) return;
        try {
            const promises = idsToFetch.map(id => getMovieApi({ movieId: id }));
            const results = await Promise.all(promises);
            const newMap = {};
            results.forEach((res, idx) => {
                const id = idsToFetch[idx];
                if (res?.success && res.data) newMap[id] = res.data;
                else newMap[id] = null;
            });
            setMovieMap(prev => ({ ...prev, ...newMap }));
        } catch (e) {
            console.error('Failed to fetch movie posters', e);
        }
    };

    const loadWatchHistory = async (page = 0, append = false) => {
        setHistoryLoading(true);
        try {
            const res = await getWatchHistoryApi({ page, size: 20 });
            if (!res?.success) {
                showToast('Failed to load watch history', res?.message || 'Unknown error', 'error');
                return;
            }
            const data = res.data || {};
            const content = data.content || [];
            setHistoryItems(prev => append ? [...prev, ...content] : content);
            setHistoryPage(data.number || page);
            setHistoryTotalPages(data.totalPages || 0);

            const ids = content.map(i => i.movieId).filter(Boolean);
            fetchPosters(ids);
        } catch (e) {
            console.error(e);
            showToast('Failed to load watch history', 'Unknown error', 'error');
        } finally {
            setHistoryLoading(false);
        }
    };

    useEffect(() => {
        if (active !== 'history') return;
        if (!isOwnProfile) return;
        loadWatchHistory(0, false);
    }, [active, isOwnProfile]);

    if (active !== 'history') return null;

    return (
        <div>
            {historyLoading && historyItems.length === 0 && <div>Loading watch history...</div>}
            {!historyLoading && historyItems.length === 0 && <p className="placeholder-note">Your watch history is empty.</p>}

            {historyItems.length > 0 && (
                <div className="watch-history-list">
                    <ul>
                        {historyItems.map(item => {
                            const movie = movieMap[item.movieId];
                            const poster = movie?.poster || null;
                            return (
                                <li key={item.id} className="watch-history-item">
                                    <Link className="watch-history-card" to={PATHS.MOVIE.DETAILS(item.movieId)} aria-label={`Open ${item.movieName || 'movie'}`}>
                                        <div className="poster">
                                            {poster ? (
                                                <img src={poster} alt={`${item.movieName} poster`} />
                                            ) : (
                                                <div className="poster-fallback">{(item.movieName || '').charAt(0)}</div>
                                            )}
                                        </div>

                                        <div className="card-body">
                                            <div className="title">{item.movieName || `Movie ${item.movieId}`}</div>
                                            <div className="muted">Watched: {item.watchedAt ? new Date(item.watchedAt).toLocaleString() : '—'}</div>
                                        </div>
                                    </Link>
                                </li>
                            );
                        })}
                    </ul>

                    {historyLoading && <div>Loading...</div>}

                    {!historyLoading && historyPage + 1 < historyTotalPages && (
                        <button className="btn btn-outline" onClick={() => loadWatchHistory(historyPage + 1, true)}>Load more</button>
                    )}
                </div>
            )}
        </div>
    );
}