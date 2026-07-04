import { useEffect, useState, useContext } from 'react';
import { Link } from 'react-router-dom';
import { History, Film } from 'lucide-react';
import { ToastContext } from '../context/ToastContext.jsx';
import { getWatchHistoryApi, getMovieApi } from '../api/movie-api.js';
import { PATHS } from '../constants/constants.jsx';
import EmptyState from './ui/EmptyState.jsx';
import Button from './ui/Button.jsx';
import Skeleton from './ui/Skeleton.jsx';
import './style/watchHistory.css';

export default function WatchHistory({ active, isOwnProfile }) {
    const { showToast } = useContext(ToastContext);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [historyItems, setHistoryItems] = useState([]);
    const [historyPage, setHistoryPage] = useState(0);
    const [historyTotalPages, setHistoryTotalPages] = useState(0);
    const [movieMap, setMovieMap] = useState({});

    const fetchPosters = async (movieIds) => {
        const idsToFetch = Array.from(new Set(movieIds)).filter((id) => id && !movieMap[id]);
        if (idsToFetch.length === 0) return;
        try {
            const results = await Promise.all(idsToFetch.map((id) => getMovieApi({ movieId: id })));
            const newMap = {};
            results.forEach((res, idx) => {
                newMap[idsToFetch[idx]] = res?.success && res.data ? res.data : null;
            });
            setMovieMap((prev) => ({ ...prev, ...newMap }));
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
            setHistoryItems((prev) => (append ? [...prev, ...content] : content));
            setHistoryPage(data.number || page);
            setHistoryTotalPages(data.totalPages || 0);

            fetchPosters(content.map((i) => i.movieId).filter(Boolean));
        } catch (e) {
            console.error(e);
            showToast('Failed to load watch history', 'Unknown error', 'error');
        } finally {
            setHistoryLoading(false);
        }
    };

    useEffect(() => {
        if (active !== 'history' || !isOwnProfile) return;
        loadWatchHistory(0, false);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [active, isOwnProfile]);

    if (active !== 'history') return null;

    return (
        <div>
            {historyLoading && historyItems.length === 0 && (
                <div className="watch-history-list">
                    <ul>
                        {Array.from({ length: 4 }).map((_, i) => (
                            <li key={i} className="watch-history-card">
                                <Skeleton variant="rect" width={80} height={120} />
                                <div className="card-body">
                                    <Skeleton variant="text" width={160} />
                                    <Skeleton variant="text" width={100} />
                                </div>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            {!historyLoading && historyItems.length === 0 && (
                <EmptyState icon={<History size={28} />} title="No watch history yet" description="Movies you watch will show up here." />
            )}

            {historyItems.length > 0 && (
                <div className="watch-history-list">
                    <ul>
                        {historyItems.map((item) => {
                            const movie = movieMap[item.movieId];
                            const poster = movie?.poster || null;
                            return (
                                <li key={item.id} className="watch-history-item">
                                    <Link className="watch-history-card" to={PATHS.MOVIE.DETAILS(item.movieId)} aria-label={`Open ${item.movieName || 'movie'}`}>
                                        <div className="poster">
                                            {poster ? (
                                                <img src={poster} alt={`${item.movieName} poster`} />
                                            ) : (
                                                <div className="poster-fallback"><Film size={24} /></div>
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

                    {historyLoading && <Skeleton variant="text" width={80} style={{ marginTop: 12 }} />}

                    {!historyLoading && historyPage + 1 < historyTotalPages && (
                        <Button variant="secondary" size="sm" onClick={() => loadWatchHistory(historyPage + 1, true)} style={{ marginTop: 12 }}>
                            Load more
                        </Button>
                    )}
                </div>
            )}
        </div>
    );
}
