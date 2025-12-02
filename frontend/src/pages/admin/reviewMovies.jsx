import "./style/reviewMoviesPage.css";
import { useState, useEffect, useContext } from "react";
import { Link, useNavigate } from "react-router-dom";
// import { AuthContext } from "../../context/authContext.jsx";
import { getPendingRequestsApi, getRequestsHistoryApi, acceptRequestApi, declineRequestApi} from "../../api/adminApi";
import {mapMovieBackendToFrontend} from "../../api/movieApi.jsx";
import NavBar from "../../components/OrgAdminNavBar.jsx";

export default function ReviewRequestsPage() {
    // const { signOut } = useContext(AuthContext);
    const navigate = useNavigate();

    const [activeTab, setActiveTab] = useState("pending"); // 'pending' or 'history'
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // const avatarMenuItems = [
    //     { label: "Sign Out", onClick: signOut },
    // ];

    // Fetch data whenever the active tab changes
    useEffect(() => {
        fetchData();
    }, [activeTab]);

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            let res;
            if (activeTab === "pending") {
                res = await getPendingRequestsApi();
                // console.log(res);
            } else {
                res = await getRequestsHistoryApi();
            }

            if (res.success) {
                setRequests(res.data);
            } else {
                setError(res.message);
            }
        } catch (err) {
            setError("Failed to load data.");
        } finally {
            setLoading(false);
        }
    };

    const handleAccept = async (requestId) => {
        if (!window.confirm("Are you sure you want to accept this request?")) return;

        const res = await acceptRequestApi({ requestId });
        if (res.success) {
            // Refresh list
            fetchData();
        } else {
            alert("Failed to accept: " + res.message);
        }
    };

    const handleDecline = async (requestId) => {
        if (!window.confirm("Are you sure you want to decline this request?")) return;

        const res = await declineRequestApi({ requestId });
        if (res.success) {
            // Refresh list
            fetchData();
        } else {
            alert("Failed to decline: " + res.message);
        }
    };

    // Navigate to MoviePreviewPage with state
    const handlePreviewMovie = (movie) => {

        const mappedMovie = mapMovieBackendToFrontend(movie);
        navigate(`/movie/${mappedMovie.id}`, { state: { movie: mappedMovie } });
    };

    return (
        <div className="review-requests-page">
            {/* Standard Navigation Bar */}
            {/* <div className="navigationBar">
                <Link to="/review-movies"><h1>Review Movies</h1></Link>
                <Link to="/admin-site-analytics"><h1>Site Movies and Analytics</h1></Link>
                <ProfileAvatar menuItems={avatarMenuItems} />
            </div> */}
            <NavBar />

            <div className="content-container">
                <div className="page-header">
                    <h2>Movie Requests</h2>

                    {/* Tabs */}
                    <div className="tabs">
                        <button
                            className={`tab-btn ${activeTab === "pending" ? "active" : ""}`}
                            onClick={() => setActiveTab("pending")}
                        >
                            Pending
                        </button>
                        <button
                            className={`tab-btn ${activeTab === "history" ? "active" : ""}`}
                            onClick={() => setActiveTab("history")}
                        >
                            History
                        </button>
                    </div>
                </div>

                {loading && <div className="loader">Loading requests...</div>}
                {error && <div className="error-message">{error}</div>}

                {/* CONTENT AREA */}
                {!loading && !error && (
                    <div className="requests-list">
                        {requests.length === 0 ? (
                            <div className="empty-state">No {activeTab} requests found.</div>
                        ) : (
                            <>
                                {activeTab === "pending" ? (
                                    <div className="pending-grid">
                                        {requests.map((req) => (
                                            <div key={req.id} className="request-card">
                                                <div className="card-header">
                                                    <span className="org-name">🏢 {req.organizationName || "Unknown Org"}</span>
                                                    <span className="date">{new Date(req.createdAt).toLocaleDateString()}</span>
                                                </div>

                                                <div className="card-body">
                                                    <h3>{req.movieName || req.movie?.title}</h3>
                                                    <p className="status-badge pending">Pending Review</p>
                                                </div>

                                                <div className="card-actions">
                                                    <button
                                                        className="btn-preview"
                                                        onClick={() => handlePreviewMovie(req.movie)}
                                                    >
                                                        👁️ Preview Movie
                                                    </button>
                                                    <div className="decision-btns">
                                                        <button
                                                            className="btn-accept"
                                                            onClick={() => handleAccept(req.id)}
                                                        >
                                                            ✓ Accept
                                                        </button>
                                                        <button
                                                            className="btn-decline"
                                                            onClick={() => handleDecline(req.id)}
                                                        >
                                                            ✕ Decline
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    /* HISTORY TABLE VIEW */
                                    <table className="history-table">
                                        <thead>
                                        <tr>
                                            <th>Date</th>
                                            <th>Movie</th>
                                            <th>Organization</th>
                                            <th>Status</th>
                                            <th>Reviewed By</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {requests.map((req) => (
                                            <tr key={req.id}>
                                                <td>{new Date(req.stateUpdatedAt || req.createdAt).toLocaleDateString()}</td>
                                                <td
                                                    className="clickable-movie"
                                                    onClick={() => handlePreviewMovie(req.movie)}
                                                >
                                                    {req.movieName || req.movie?.title} ↗
                                                </td>
                                                <td>{req.organizationName || "N/A"}</td>
                                                <td>
                                                        <span className={`status-badge ${req.state.toLowerCase()}`}>
                                                            {req.state}
                                                        </span>
                                                </td>
                                                <td>{req.admin?.name || "Admin"}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}