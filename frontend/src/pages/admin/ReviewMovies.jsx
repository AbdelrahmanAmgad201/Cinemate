import { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { Building2, Eye, Check, X, Inbox } from 'lucide-react';

import NavBar from '../../components/OrgAdminNavBar.jsx';
import './style/ReviewMoviesPage.css';
import { getPendingRequestsApi, getRequestsHistoryApi, acceptRequestApi, declineRequestApi } from '../../api/admin-api.js';
import { mapMovieBackendToFrontend } from '../../utils/api-mappers.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { PATHS } from '../../constants/constants.jsx';
import Tabs from '../../components/ui/Tabs.jsx';
import Card from '../../components/ui/Card.jsx';
import Badge from '../../components/ui/Badge.jsx';
import Button from '../../components/ui/Button.jsx';
import ConfirmDialog from '../../components/ui/ConfirmDialog.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import LoadingFallback from '../../components/LoadingFallback.jsx';

const TABS = [
    { key: 'pending', label: 'Pending' },
    { key: 'history', label: 'History' },
];

const STATUS_VARIANT = { PENDING: 'warning', ACCEPTED: 'success', DECLINED: 'error' };

export default function ReviewRequestsPage() {
    const { showToast } = useContext(ToastContext);
    const navigate = useNavigate();

    const [activeTab, setActiveTab] = useState('pending');
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [confirmAction, setConfirmAction] = useState(null);

    useEffect(() => {
        fetchData();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeTab]);

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = activeTab === 'pending' ? await getPendingRequestsApi() : await getRequestsHistoryApi();
            if (res.success) setRequests(res.data);
            else setError(res.message);
        } catch (err) {
            setError('Failed to load data. ' + err);
        } finally {
            setLoading(false);
        }
    };

    const handleAccept = async (requestId) => {
        setConfirmAction(null);
        const res = await acceptRequestApi({ requestId });
        if (res.success) fetchData();
        else showToast('Failed to accept request', res.message, 'error');
    };

    const handleDecline = async (requestId) => {
        setConfirmAction(null);
        const res = await declineRequestApi({ requestId });
        if (res.success) fetchData();
        else showToast('Failed to decline request', res.message, 'error');
    };

    const handlePreviewMovie = (movie) => {
        const mappedMovie = mapMovieBackendToFrontend(movie);
        navigate(PATHS.MOVIE.DETAILS(mappedMovie.id), { state: { movie: mappedMovie } });
    };

    return (
        <div className="review-requests-page">
            <NavBar />

            <div className="content-container">
                <div className="page-header">
                    <h2>Movie requests</h2>
                    <Tabs tabs={TABS} activeId={activeTab} onChange={setActiveTab} />
                </div>

                {loading && <LoadingFallback />}
                {!loading && error && <EmptyState title="Couldn't load requests" description={error} />}

                {!loading && !error && requests.length === 0 && (
                    <EmptyState icon={<Inbox size={28} />} title={`No ${activeTab} requests found`} />
                )}

                {!loading && !error && requests.length > 0 && (
                    activeTab === 'pending' ? (
                        <div className="pending-grid">
                            {requests.map((req) => (
                                <Card key={req.id} padding="md" className="request-card">
                                    <div className="card-header">
                                        <span className="org-name"><Building2 size={14} /> {req.organizationName || 'Unknown Org'}</span>
                                        <span className="date">{new Date(req.createdAt).toLocaleDateString()}</span>
                                    </div>

                                    <div className="card-body">
                                        <h3>{req.movieName || req.movie?.title}</h3>
                                        <Badge variant="warning">Pending review</Badge>
                                    </div>

                                    <div className="card-actions">
                                        <Button variant="secondary" size="sm" icon={<Eye size={14} />} onClick={() => handlePreviewMovie(req.movie)}>
                                            Preview movie
                                        </Button>
                                        <div className="decision-btns">
                                            <Button size="sm" fullWidth icon={<Check size={14} />} onClick={() => setConfirmAction({ type: 'accept', id: req.id })}>
                                                Accept
                                            </Button>
                                            <Button variant="danger" size="sm" fullWidth icon={<X size={14} />} onClick={() => setConfirmAction({ type: 'decline', id: req.id })}>
                                                Decline
                                            </Button>
                                        </div>
                                    </div>
                                </Card>
                            ))}
                        </div>
                    ) : (
                        <table className="history-table">
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Movie</th>
                                    <th>Organization</th>
                                    <th>Status</th>
                                    <th>Reviewed by</th>
                                </tr>
                            </thead>
                            <tbody>
                                {requests.map((req) => (
                                    <tr key={req.id}>
                                        <td>{new Date(req.stateUpdatedAt || req.createdAt).toLocaleDateString()}</td>
                                        <td className="clickable-movie" onClick={() => handlePreviewMovie(req.movie)}>
                                            {req.movieName || req.movie?.title}
                                        </td>
                                        <td>{req.organizationName || 'N/A'}</td>
                                        <td><Badge variant={STATUS_VARIANT[req.state] || 'neutral'}>{req.state}</Badge></td>
                                        <td>{req.admin?.name || 'Admin'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )
                )}
            </div>

            <ConfirmDialog
                open={!!confirmAction}
                onClose={() => setConfirmAction(null)}
                onConfirm={() => (confirmAction?.type === 'accept' ? handleAccept(confirmAction.id) : handleDecline(confirmAction.id))}
                title={confirmAction?.type === 'accept' ? 'Accept request?' : 'Decline request?'}
                message={`Are you sure you want to ${confirmAction?.type === 'accept' ? 'accept' : 'decline'} this request?`}
                confirmLabel={confirmAction?.type === 'accept' ? 'Accept' : 'Decline'}
                danger={confirmAction?.type === 'decline'}
            />
        </div>
    );
}
