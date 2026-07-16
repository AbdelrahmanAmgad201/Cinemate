import './style/orgAnalytics.css';
import { useEffect, useState } from 'react';
import NavBar from '../../components/OrgAdminNavBar.jsx';
import { fetchOrgAnalyticsApi, fetchOrgRequestsApi, fetchOrgMoviesApi } from '../../api/org-analytics-api.js';
import MoviesList from '../../components/MoviesList.jsx';
import Skeleton from '../../components/ui/Skeleton.jsx';
import Badge from '../../components/ui/Badge.jsx';

export const DashboardCard = ({ title, value, subtitle, children }) => (
    <div className="stat-card">
        <div className="stat-card-header">
            <h3>{title}</h3>
            {subtitle ? <span className="stat-subtitle">{subtitle}</span> : null}
        </div>
        {typeof value !== 'undefined' && value !== null ? (
            <div className="stat-value">{value}</div>
        ) : null}
        {children}
    </div>
);

const RequestBreakdown = ({ approved = 0, rejected = 0, pending = 0 }) => {
    const total = approved + rejected + pending || 1;
    const percent = {
        approved: Math.round((approved / total) * 100),
        rejected: Math.round((rejected / total) * 100),
        pending: Math.round((pending / total) * 100),
    };
    return (
        <div className="requests">
            <div className="requests-bar" role="progressbar" aria-valuemin={0} aria-valuemax={100}>
                <div className="approved" style={{ width: `${percent.approved}%` }} />
                <div className="pending" style={{ width: `${percent.pending}%` }} />
                <div className="rejected" style={{ width: `${percent.rejected}%` }} />
            </div>
            <div className="requests-legend">
                <span className="legend-item"><span className="dot approved" /> <span className="legend-text">Approved:</span> <span className="legend-value">{approved}</span></span>
                <span className="legend-item"><span className="dot pending" /> <span className="legend-text">Pending:</span> <span className="legend-value">{pending}</span></span>
                <span className="legend-item"><span className="dot rejected" /> <span className="legend-text">Rejected:</span> <span className="legend-value">{rejected}</span></span>
            </div>
        </div>
    );
};

const MovieRequestsList = ({ requests = [], loading = false }) => {
    const [showAll, setShowAll] = useState(false);

    if (loading) {
        return (
            <div className="movie-requests-list">
                {[1, 2, 3].map((i) => <Skeleton key={i} variant="rect" height={56} />)}
            </div>
        );
    }

    if (!requests || requests.length === 0) {
        return <div className="no-requests">No movie requests yet</div>;
    }

    const statusVariant = { ACCEPTED: 'success', REJECTED: 'error', PENDING: 'warning' };
    const statusLabel = { ACCEPTED: 'Approved', REJECTED: 'Rejected', PENDING: 'Pending' };

    const displayedRequests = showAll ? requests : requests.slice(0, 5);
    const hasMore = requests.length > 5;

    return (
        <div className="movie-requests-list">
            {displayedRequests.map((request) => (
                <div key={request.id} className="movie-request-item">
                    <div className="movie-request-info">
                        <span className="movie-name">{request.movieName}</span>
                        <span className="request-date">{new Date(request.createdAt).toLocaleDateString()}</span>
                    </div>
                    <Badge variant={statusVariant[request.state] || 'neutral'}>{statusLabel[request.state] || 'Unknown'}</Badge>
                </div>
            ))}
            {hasMore && (
                <button type="button" className="view-all-link" onClick={() => setShowAll(!showAll)}>
                    {showAll ? 'Show less' : `+ ${requests.length - 5} more requests`}
                </button>
            )}
        </div>
    );
};

const OrgMoviesAndAnalytics = () => {
    const [loading, setLoading] = useState(true);
    const [analytics, setAnalytics] = useState(null);
    const [movieRequests, setMovieRequests] = useState([]);
    const [myMovies, setMyMovies] = useState([]);
    const [requestsLoading, setRequestsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        const loadAnalytics = async () => {
            try {
                setLoading(true);
                const { moviesOverview, requestsOverview } = await fetchOrgAnalyticsApi();
                setAnalytics({
                    totalMovies: moviesOverview.numberOfMovies,
                    totalViews: moviesOverview.totalViewsAcrossAllMovies,
                    popularGenre: moviesOverview.mostPopularGenre,
                    requests: {
                        approved: requestsOverview.numberOfAccepted || 0,
                        rejected: requestsOverview.numberOfRejected || 0,
                        pending: requestsOverview.numberOfPendings || 0,
                    },
                });
            } catch (err) {
                console.error('Error loading analytics:', err);
            } finally {
                setLoading(false);
            }
        };

        const loadRequests = async () => {
            try {
                setRequestsLoading(true);
                setMovieRequests(await fetchOrgRequestsApi());
            } catch (err) {
                console.error('Error loading requests:', err);
            } finally {
                setRequestsLoading(false);
            }
        };

        loadAnalytics();
        loadRequests();
    }, []);

    useEffect(() => {
        const loadMovies = async () => {
            try {
                setLoading(true);
                const result = await fetchOrgMoviesApi(page, 6);
                if (result.success) {
                    setMyMovies(result.response.content);
                    setTotalPages(result.response.totalPages);
                }
            } catch (err) {
                console.error('Error fetching movies:', err);
            } finally {
                setLoading(false);
            }
        };
        loadMovies();
    }, [page]);

    const data = analytics || {};

    const movieCards = myMovies.map((movie) => ({
        id: movie.movieID ?? movie.id,
        title: movie.name,
        poster: movie.thumbnailUrl,
        duration: movie.duration,
        rating: movie.averageRating,
    }));

    return (
        <div className="org-analytics-page">
            <NavBar />
            <div className={`analytics-grid ${loading ? 'loading' : ''}`}>
                <DashboardCard title="Total Movies Added" value={loading ? '—' : data.totalMovies} />
                <DashboardCard title="Total Views" value={loading ? '—' : data.totalViews?.toLocaleString?.() ?? data.totalViews} />
                <DashboardCard title="Most Popular Genre" value={null}>
                    {loading ? <Skeleton variant="rect" height={28} width={120} /> : <span className="genre-badge">{data.popularGenre || '—'}</span>}
                </DashboardCard>
                <DashboardCard title="Requests" value={null}>
                    {loading ? <Skeleton variant="rect" height={48} /> : (
                        <RequestBreakdown approved={data?.requests?.approved} rejected={data?.requests?.rejected} pending={data?.requests?.pending} />
                    )}
                </DashboardCard>
            </div>
            <div className="full-width-section">
                <DashboardCard title="Recent Movie Requests" value={null}>
                    <MovieRequestsList requests={movieRequests} loading={requestsLoading} />
                </DashboardCard>
            </div>
            <div className="full-width-section">
                <DashboardCard title="Movies Approved" value={null}>
                    <MoviesList list={movieCards} page={page} setPage={setPage} totalPages={totalPages} loading={loading} />
                </DashboardCard>
            </div>
        </div>
    );
};

export default OrgMoviesAndAnalytics;
