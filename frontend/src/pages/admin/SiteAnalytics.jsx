import './style/SiteAnalytics.css';
import './style/NavBar.css';

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Film, Building2, Heart, BarChart3, ArrowUpRight } from 'lucide-react';
import NavBar from '../../components/OrgAdminNavBar.jsx';

import { getSystemAnalyticsApi } from '../../api/admin-api.js';
import { getMovieApi } from '../../api/movie-api.js';
import { PATHS } from '../../constants/constants.jsx';
import StatCard from '../../components/ui/StatCard.jsx';
import Card from '../../components/ui/Card.jsx';
import Button from '../../components/ui/Button.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import LoadingFallback from '../../components/LoadingFallback.jsx';

export default function SiteAnalytics() {
    const navigate = useNavigate();

    const [systemData, setSystemData] = useState(null);
    const [mostLikedName, setMostLikedName] = useState('Loading…');
    const [mostRatedName, setMostRatedName] = useState('Loading…');

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            const res = await getSystemAnalyticsApi();

            if (res.success) {
                const data = res.data;
                setSystemData(data);

                if (data.mostLikedMovieId) {
                    const likedRes = await getMovieApi({ movieId: data.mostLikedMovieId });
                    setMostLikedName(likedRes.success ? likedRes.data.title : 'Unknown movie');
                } else {
                    setMostLikedName('N/A');
                }

                if (data.mostRatedMovieId) {
                    const ratedRes = await getMovieApi({ movieId: data.mostRatedMovieId });
                    setMostRatedName(ratedRes.success ? ratedRes.data.title : 'Unknown movie');
                } else {
                    setMostRatedName('N/A');
                }
            } else {
                setError(res.message || 'Failed to load system analytics');
            }
            setLoading(false);
        };

        fetchData();
    }, []);

    const handlePreviewMovie = (id) => {
        if (id) navigate(PATHS.MOVIE.DETAILS(id));
    };

    return (
        <div className="analytics-page">
            <NavBar />

            <div className="content-container">
                <div className="page-header">
                    <h2>System overview</h2>
                    <p className="subtitle">Platform performance and engagement metrics</p>
                </div>

                {loading && <LoadingFallback />}
                {!loading && error && <EmptyState title="Couldn't load analytics" description={error} />}

                {!loading && systemData && (
                    <div className="analytics-dashboard">
                        <div className="stats-grid">
                            <StatCard icon={<Users size={20} />} label="Total users" value={systemData.numberOfUsers?.toLocaleString() ?? 0} />
                            <StatCard icon={<Film size={20} />} label="Total movies" value={systemData.numberOfMovies?.toLocaleString() ?? 0} />
                        </div>

                        <h3 className="section-title">Platform highlights</h3>

                        <div className="highlights-grid">
                            <Card padding="lg" className="highlight-card">
                                <div className="card-top"><Building2 size={16} /><span>Top organization</span></div>
                                <h3>{systemData.mostPopularOrgName}</h3>
                            </Card>

                            <Card padding="lg" className="highlight-card">
                                <div className="card-top"><Heart size={16} /><span>Most liked movie</span></div>
                                <h3>{mostLikedName}</h3>
                                {systemData.mostLikedMovieId && (
                                    <Button variant="secondary" size="sm" icon={<ArrowUpRight size={14} />} onClick={() => handlePreviewMovie(systemData.mostLikedMovieId)}>
                                        View movie page
                                    </Button>
                                )}
                            </Card>

                            <Card padding="lg" className="highlight-card">
                                <div className="card-top"><BarChart3 size={16} /><span>Most rated movie</span></div>
                                <h3>{mostRatedName}</h3>
                                {systemData.mostRatedMovieId && (
                                    <Button variant="secondary" size="sm" icon={<ArrowUpRight size={14} />} onClick={() => handlePreviewMovie(systemData.mostRatedMovieId)}>
                                        View movie page
                                    </Button>
                                )}
                            </Card>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
