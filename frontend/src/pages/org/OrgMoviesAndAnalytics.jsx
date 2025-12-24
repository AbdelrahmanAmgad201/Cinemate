import '../auth/style/SignUp.css';
import { Link } from 'react-router-dom';
import './style/orgAnalytics.css';
import {use, useContext, useEffect, useState} from 'react';
import NavBar from "../../components/OrgAdminNavBar.jsx";
import { fetchOrgAnalytics, fetchOrgRequests, fetchOrgMovies } from '../../api/org-analytics-api.jsx';
import {AuthContext} from "../../context/AuthContext.jsx";
import {PATHS} from "../../constants/constants.jsx";
import MoviesList from '../../components/MoviesList.jsx';
import { MdOutlineStar, MdNavigateNext, MdNavigateBefore } from "react-icons/md";

export const StatCard = ({ title, value, subtitle, children }) => (
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
    const [myMovies, setMyMovies] = useState([]);
    const [requestsLoading, setRequestsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
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

    
    useEffect(() => {
        const fetchMovies = async () => {
            try {
                setLoading(true);
                const result = await fetchOrgMovies(page, 6);
                if(result.success){
                    setMyMovies(result.response.content);
                    setTotalPages(result.response.totalPages);
                }
                console.log("Fetched movies:", result.response);
            } catch (err) {
                console.error("Error fetching movies:", err);
            } finally {
                setLoading(false);
            }
        }
        fetchMovies();
    }, [page]);

    const data = analytics || {};


    return (
        <div className="org-analytics-page">
            <NavBar />
            <div className={`analytics-grid ${loading ? 'loading' : ''}`}>
                <StatCard title="Total Movies Added" value={loading ? '—' : data.totalMovies} />
                <StatCard title="Total Views" value={loading ? '—' : data.totalViews?.toLocaleString?.() ?? data.totalViews} />
                <StatCard title="Most Popular Genre" value={null}>
                    {loading ? <div className="skeleton skeleton-chip" /> : <GenreBadge genre={data.popularGenre} />}
                </StatCard>
                <StatCard title="Requests" value={null}>
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
                    {/* <MovieRequestsList requests={movieRequests} loading={requestsLoading} /> */}
                </StatCard>
            </div>
            <div className="full-width-section">
                <div className="movie-list">
                <StatCard title="Movies Approved">
                <div className="list-container">
                    {
                    myMovies.length === 0 ? (
                        <div style={{marginTop: "100px"}}>
                            <p style={{fontSize: "30px"}}>Nothing Here...</p>
                        </div>
                    ) : (
                        <>
                        {myMovies.map((movie, index) => (
                            <div key={index} className="movie-item">
                                <img src={movie.thumbnailUrl} alt={movie.name} className="movie-poster" />
                                <div className="movie-info">
                                <h3 className="movie-title">{movie.name}</h3>
                                <div className="movie-details">
                                    <div style={{marginBottom: "10px"}} className="duration">{Math.floor(movie.duration/60)}h {movie.duration%60}min</div>
                                    <div className="rating" style={{display: "flex",alignItems: "center", gap: "6px"}}><MdOutlineStar style={{color: "#ffc107"}}/> {movie.averageRating}/10</div>
                                </div>
                                </div>
                            </div>
                            ))}
                        {totalPages > 1 &&(
                            <div className="paging">
                                <MdNavigateBefore className={page > 0 ? "paging-icon" : "inactive"} onClick={() => page > 0 && setPage(page - 1)} />
                                {page + 1}
                                <MdNavigateNext className={page < totalPages - 1 ? "paging-icon" : "inactive"} onClick={() => page < totalPages - 1  && setPage(page + 1)} />
                            </div>
                        )}
                        </>
                    )
                    }
                    </div>
                </StatCard>
                </div>
            </div>
        </div>
    );
};

export default OrgMoviesAndAnalytics;