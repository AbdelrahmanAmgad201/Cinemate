import { useEffect, useState, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Users } from 'lucide-react';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { getFollowingApi, followUserApi, unfollowUserApi } from '../../api/user-api.js';
import Button from '../../components/ui/Button.jsx';
import IconButton from '../../components/ui/IconButton.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import LoadingFallback from '../../components/LoadingFallback.jsx';
import FollowListLink from '../../components/ui/FollowListLink.jsx';
import './style/FollowListPage.css';

export default function FollowingPage() {
    const { userId } = useParams();
    const navigate = useNavigate();
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    const [loading, setLoading] = useState(true);
    const [items, setItems] = useState([]);
    const [error, setError] = useState(null);
    const isOwnProfile = user && user.id === Number(userId);

    useEffect(() => {
        if (!isOwnProfile) {
            showToast('Access denied', 'You can only view your own following list.', 'warning');
            navigate(-1);
            return;
        }

        let cancelled = false;
        setLoading(true);
        setError(null);

        getFollowingApi({ page: 0, size: 100 }).then((res) => {
            if (cancelled) return;
            if (res.success && res.data) {
                const followings = res.data.content || [];

                const mapped = followings.map((following) => {
                    const followedUser = following.followedUser;
                    if (!followedUser) return null;

                    return {
                        id: followedUser.id,
                        username: `${followedUser.firstName || ''} ${followedUser.lastName || ''}`.trim() || `User ${followedUser.id}`,
                        isFollowed: true,
                    };
                }).filter((item) => item !== null);

                setItems(mapped);
            } else {
                setError(res.message || 'Following list is not available');
            }
        }).catch((err) => {
            if (!cancelled) setError(err?.message || 'Failed to load following');
        }).finally(() => { if (!cancelled) setLoading(false); });

        return () => { cancelled = true; };
    }, [userId, user, isOwnProfile, navigate, showToast]);

    const handleFollowToggle = async (targetId, currentlyFollowing) => {
        try {
            const res = currentlyFollowing ? await unfollowUserApi({ userId: targetId }) : await followUserApi({ userId: targetId });
            if (!res.success) throw new Error(res.message || 'Action failed');
            setItems((prev) => prev.map((it) => (it.id === targetId ? { ...it, isFollowed: !currentlyFollowing } : it)));
        } catch (err) {
            showToast('Error', err?.message || 'Failed to perform follow action', 'error');
        }
    };

    if (loading) return <LoadingFallback fullScreen />;

    return (
        <div className="follow-list-page">
            <div className="follow-list-header">
                <IconButton label="Go back" onClick={() => navigate(-1)}><ArrowLeft size={18} /></IconButton>
                <h1>Following</h1>
            </div>

            {error && <EmptyState title="Couldn't load following list" description={error} />}

            {!error && items.length === 0 && (
                <EmptyState icon={<Users size={28} />} title="Not following anyone yet" />
            )}

            {!error && items.length > 0 && (
                <div className="follow-list">
                    {items.map((it) => (
                        <FollowListLink
                            key={it.id}
                            userId={it.id}
                            username={it.username}
                            action={(
                                <Button
                                    size="sm"
                                    variant={it.isFollowed ? 'secondary' : 'primary'}
                                    onClick={() => handleFollowToggle(it.id, Boolean(it.isFollowed))}
                                >
                                    {it.isFollowed ? 'Unfollow' : 'Follow'}
                                </Button>
                            )}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
