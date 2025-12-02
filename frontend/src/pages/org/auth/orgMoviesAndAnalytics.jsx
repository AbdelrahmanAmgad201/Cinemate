import '../../auth/style/signUp.css';
import { Link } from 'react-router-dom';
import '../style/orgAnalytics.css';
import {useContext, useEffect, useState} from 'react';
import fetchOrgAnalytics, { fetchOrgRequests } from '../../../api/orgAnalyticsApi';
// import ProfileAvatar from "../../../components/profileAvatar.jsx";
import {AuthContext} from "../../../context/authContext.jsx";
import NavBar from "../../../components/OrgAdminNavBar.jsx";

const StatCard = ({ title, value, subtitle, children }) => (
    <div className="stat-card">
        <div className="stat-card-header">
            <h3>{title}</h3>
            {subtitle ? <span className="stat-subtitle">{subtitle}</span> : null}
        </div>
        <div className="stat-value">{value ?? '--'}</div>
        {children}
    </div>
);

const Stars = ({ value = 0, outOf = 5 }) => {
    const full = Math.floor(value);
    const half = value - full >= 0.5;
    return (
        <div className="stars">
            {Array.from({ length: outOf }).map((_, i) => {
                const isFull = i < full;
                const isHalf = !isFull && i === full && half;
                return (
                    <span key={i} className={`star ${isFull ? 'full' : ''} ${isHalf ? 'half' : ''}`}>★</span>
                );
            })}
            <span className="stars-number">{value?.toFixed ? value.toFixed(2) : value}</span>
        </div>
    );
};

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
                <div className="rejected" style={{ width: `${percent.rejected}%` }} />
                <div className="pending" style={{ width: `${percent.pending}%` }} />
            </div>
            <div className="requests-legend">
                <span className="dot approved" /> Approved: {approved}
                <span className="dot rejected" /> Rejected: {rejected}
                <span className="dot pending" /> Pending: {pending}
            </div>
        </div>
    );
};

const GenreBadge = ({ genre }) => (
    <span className="genre-badge">{genre || '—'}</span>
);

const MovieRequestsList = ({ requests = [], loading = false }) => {
    const [showAll, setShowAll] = useState(false);

    if (loading) {
        return (
            <div className="movie-requests-list">
                {[1, 2, 3].map(i => (
                    <div key={i} className="skeleton skeleton-movie-item" />
                ))}
            </div>
        );
    }

    if (!requests || requests.length === 0) {
        return <div className="no-requests">No movie requests yet</div>;
    }

    const getStatusClass = (state) => {
        switch (state) {
            case 'ACCEPTED': return 'approved';
            case 'REJECTED': return 'rejected';
            case 'PENDING': return 'pending';
            default: return 'pending';
        }
    };

    const getStatusLabel = (state) => {
        switch (state) {
            case 'ACCEPTED': return 'Approved';
            case 'REJECTED': return 'Rejected';
            case 'PENDING': return 'Pending';
            default: return 'Unknown';
        }
    };

    const displayedRequests = showAll ? requests : requests.slice(0, 5);
    const hasMore = requests.length > 5;

    return (
        <div className="movie-requests-list">
            {displayedRequests.map((request) => (
                <div key={request.id} className="movie-request-item">
                    <div className="movie-request-info">
                        <span className="movie-name">{request.movieName}</span>
                        <span className="request-date">
                            {new Date(request.createdAt).toLocaleDateString()}
                        </span>
                    </div>
                    <span className={`status-badge ${getStatusClass(request.state)}`}>
                        {getStatusLabel(request.state)}
                    </span>
                </div>
            ))}
            {hasMore && (
                <div className="view-all-link" onClick={() => setShowAll(!showAll)}>
                    {showAll ? 'Show less' : `+ ${requests.length - 5} more requests`}
                </div>
            )}
        </div>
    );
};

const OrgMoviesAndAnalytics = () => {
    const [loading, setLoading] = useState(true);
    const [analytics, setAnalytics] = useState(null);
    const [movieRequests, setMovieRequests] = useState([]);
    const [requestsLoading, setRequestsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const loadAnalytics = async () => {
            try {
                setLoading(true);
                const { moviesOverview, requestsOverview } = await fetchOrgAnalytics();
                setAnalytics({
                    totalMovies: moviesOverview.numberOfMovies,
                    totalViews: moviesOverview.totalViewsAcrossAllMovies,
                    popularGenre: moviesOverview.mostPopularGenre,
                    requests: {
                        approved: requestsOverview.numberOfAccepted || 0,
                        rejected: requestsOverview.numberOfRejected || 0,
                        pending: requestsOverview.numberOfPendings || 0
                    }
                });
            } catch (err) {
                console.error("Error loading analytics:", err);
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        const loadRequests = async () => {
            try {
                setRequestsLoading(true);
                const requests = await fetchOrgRequests();
                setMovieRequests(requests);
            } catch (err) {
                console.error("Error loading requests:", err);
            } finally {
                setRequestsLoading(false);
            }
        };

        loadAnalytics();
        loadRequests();
    }, []);

    const data = analytics || {};

    const {signOut} = useContext(AuthContext);
    const avatarMenuItems = [
        // { label: "Profile", onClick: () => console.log("Profile clicked") },
        // { label: "Settings", onClick: () => console.log("Settings clicked") },
        { label: "Sign Out", onClick: signOut },
    ];

    return (
        <div className="org-analytics-page">
            {/* <div className="navigationBar">
                // <Link to="/"><h1>Home Page</h1></Link>
                <Link to="/org-add-movie"><h1>Add Movie</h1></Link>
                <Link to="/org-movies-and-analytics"><h1>My Movies and Analytics</h1></Link>
                <ProfileAvatar menuItems={avatarMenuItems} />
            </div> */}
            <NavBar />
            <div className={`analytics-grid ${loading ? 'loading' : ''}`}>
                <StatCard title="Total Movies Added" value={loading ? '—' : data.totalMovies} />
                <StatCard title="Total Views" value={loading ? '—' : data.totalViews?.toLocaleString?.() ?? data.totalViews} />
                <StatCard title="Most Popular Genre" value={null}>
                    {loading ? <div className="skeleton skeleton-chip" /> : <GenreBadge genre={data.popularGenre} />}
                </StatCard>
                <StatCard title="Requests" subtitle="Approved / Rejected / Pending" value={null}>
                    {loading ? (
                        <div className="skeleton skeleton-block" />
                    ) : (
                        <RequestBreakdown
                            approved={data?.requests?.approved}
                            rejected={data?.requests?.rejected}
                            pending={data?.requests?.pending}
                        />
                    )}
                </StatCard>
            </div>
            <div className="full-width-section">
                <StatCard title="Recent Movie Requests" value={null}>
                    <MovieRequestsList requests={movieRequests} loading={requestsLoading} />
                </StatCard>
            </div>
        </div>
    );
};

export default OrgMoviesAndAnalytics;