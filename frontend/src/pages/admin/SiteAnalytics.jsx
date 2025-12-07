import "./style/SiteAnalytics.css";
import "./style/NavBar.css";

import { useState, useEffect, useContext } from "react";
import { Link, useNavigate } from "react-router-dom";

import { AuthContext } from "../../context/AuthContext.jsx";
import ProfileAvatar from "../../components/ProfileAvatar.jsx";
import { getSystemAnalyticsApi } from "../../api/admin-api.jsx";
import { getMovieApi } from "../../api/movie-api.jsx";
import {PATHS} from "../../constants/constants.jsx";


const icons = {
    users: "👥",
    movies: "🎬",
    org: "🏢",
    star: "⭐",
    like: "❤️",
    rate: "📊"
};

export default function SiteAnalytics() {
    const { signOut } = useContext(AuthContext);
    const navigate = useNavigate();

    const [systemData, setSystemData] = useState(null);
    // New state to hold the fetched names
    const [mostLikedName, setMostLikedName] = useState("Loading...");
    const [mostRatedName, setMostRatedName] = useState("Loading...");

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const avatarMenuItems = [
        { label: "Sign Out", onClick: signOut },
    ];

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);

            // 1. Fetch the main analytics data
            const res = await getSystemAnalyticsApi();

            if (res.success) {
                const data = res.data;
                setSystemData(data);

                // 2. Fetch the "Most Liked" Movie Name
                if (data.mostLikedMovieId) {
                    const likedRes = await getMovieApi({ movieId: data.mostLikedMovieId });
                    if (likedRes.success) {
                        setMostLikedName(likedRes.data.title); // Assuming mapped object has .title
                    } else {
                        setMostLikedName("Unknown Movie");
                    }
                } else {
                    setMostLikedName("N/A");
                }

                // 3. Fetch the "Most Rated" Movie Name
                if (data.mostRatedMovieId) {
                    const ratedRes = await getMovieApi({ movieId: data.mostRatedMovieId });
                    if (ratedRes.success) {
                        setMostRatedName(ratedRes.data.title);
                    } else {
                        setMostRatedName("Unknown Movie");
                    }
                } else {
                    setMostRatedName("N/A");
                }

            } else {
                setError(res.message || "Failed to load system analytics");
            }
            setLoading(false);
        };

        fetchData();
    }, []);

    const handlePreviewMovie = (id) => {
        if (!id) return;
        navigate(PATHS.MOVIE.DETAILS(id));
    };

    return (
        <div className="analytics-page">
            <div className="navigationBar">
                <Link to={PATHS.ADMIN.REVIEW_REQUESTS} ><h1>Review Movies</h1></Link>
                <Link to={PATHS.ADMIN.SITE_ANALYTICS} ><h1>Site Movies and Analytics</h1></Link>
                <Link to={PATHS.ADMIN.ADD_ADMIN} ><h1>Add New Admin</h1></Link>
                <ProfileAvatar menuItems={avatarMenuItems} />
            </div>

            <div className="content-container">
                <div className="page-header">
                    <h2>System Overview</h2>
                    <p className="subtitle">Platform performance and engagement metrics</p>
                </div>

                {loading && <div className="loader">Loading analytics...</div>}
                {error && <div className="error-card">⚠️ {error}</div>}

                {!loading && systemData && (
                    <div className="analytics-dashboard">

                        {/* 1. General Counts */}
                        <div className="stats-grid">
                            <div className="stat-card">
                                <div className="icon-wrapper green">{icons.users}</div>
                                <div className="stat-info">
                                    <span className="stat-label">Total Users</span>
                                    <span className="stat-value">{systemData.numberOfUsers?.toLocaleString() ?? 0}</span>
                                </div>
                            </div>

                            <div className="stat-card">
                                <div className="icon-wrapper blue">{icons.movies}</div>
                                <div className="stat-info">
                                    <span className="stat-label">Total Movies</span>
                                    <span className="stat-value">{systemData.numberOfMovies?.toLocaleString() ?? 0}</span>
                                </div>
                            </div>
                        </div>

                        <h3 className="section-title">Platform Highlights</h3>

                        {/* 2. Highlights Grid */}
                        <div className="highlights-grid">

                            {/* Most Popular Organization */}
                            <div className="highlight-card org-card">
                                <div className="card-top">
                                    <span className="card-icon">{icons.org}</span>
                                    <span className="card-label">Top Organization</span>
                                </div>
                                <div className="card-main">
                                    <h3>{systemData.mostPopularOrgName}</h3>
                                </div>
                            </div>

                            {/* Most Liked Movie */}
                            <div className="highlight-card movie-card">
                                <div className="card-top">
                                    <span className="card-icon">{icons.like}</span>
                                    <span className="card-label">Most Liked Movie</span>
                                </div>
                                <div className="card-main">
                                    {/* Display the FETCHED NAME instead of ID */}
                                    <h3>{mostLikedName}</h3>

                                    {systemData.mostLikedMovieId && (
                                        <button
                                            className="preview-link-btn"
                                            onClick={() => handlePreviewMovie(systemData.mostLikedMovieId)}
                                        >
                                            View Movie Page ↗
                                        </button>
                                    )}
                                </div>
                            </div>

                            {/* Most Rated Movie */}
                            <div className="highlight-card movie-card">
                                <div className="card-top">
                                    <span className="card-icon">{icons.rate}</span>
                                    <span className="card-label">Most Rated Movie</span>
                                </div>
                                <div className="card-main">
                                    {/* Display the FETCHED NAME instead of ID */}
                                    <h3>{mostRatedName}</h3>

                                    {systemData.mostRatedMovieId && (
                                        <button
                                            className="preview-link-btn"
                                            onClick={() => handlePreviewMovie(systemData.mostRatedMovieId)}
                                        >
                                            View Movie Page ↗
                                        </button>
                                    )}
                                </div>
                            </div>

                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}