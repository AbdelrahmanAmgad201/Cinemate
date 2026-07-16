import { useEffect, useState, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Users } from 'lucide-react';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { getFollowersApi, followUserApi, unfollowUserApi, isUserFollowedApi } from '../../api/user-api.js';
import { PATHS } from '../../constants/constants.jsx';
import Avatar from '../../components/ui/Avatar.jsx';
import Button from '../../components/ui/Button.jsx';
import IconButton from '../../components/ui/IconButton.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import LoadingFallback from '../../components/LoadingFallback.jsx';
import FollowListLink from '../../components/ui/FollowListLink.jsx';
import './style/FollowListPage.css';

export default function FollowersPage() {
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
            showToast('Access denied', 'You can only view your own followers list.', 'warning');
            navigate(-1);
            return;
        }

        let cancelled = false;
        setLoading(true);
        setError(null);

        getFollowersApi({ page: 0, size: 100 }).then(async (res) => {
            if (cancelled) return;
            if (res.success && res.data) {
                const followers = res.data.content || [];

                const mapped = await Promise.all(followers.map(async (follower) => {
                    const followingUser = follower.followingUser;
                    if (!followingUser) return null;

                    const followStatus = await isUserFollowedApi({ userId: followingUser.id });

                    return {
                        id: followingUser.id,
                        username: `${followingUser.firstName || ''} ${followingUser.lastName || ''}`.trim() || `User ${followingUser.id}`,
                        isFollowed: followStatus.success ? followStatus.data : false,
                    };
                }));

                setItems(mapped.filter((item) => item !== null));
            } else {
                setError(res.message || 'Followers list is not available');
            }
        }).catch((err) => {
            if (!cancelled) setError(err?.message || 'Failed to load followers');
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
                <h1>Followers</h1>
            </div>

            {error && <EmptyState title="Couldn't load followers" description={error} />}

            {!error && items.length === 0 && (
                <EmptyState icon={<Users size={28} />} title="No followers yet" />
            )}

            {!error && items.length > 0 && (
                <div className="follow-list">
                    {items.map((it) => (
                        <FollowListLink
                            key={it.id}
                            userId={it.id}
                            username={it.username}
                            action={it.id !== user?.id && (
                                <Button
                                    size="sm"
                                    variant={it.isFollowed ? 'secondary' : 'primary'}
                                    onClick={() => handleFollowToggle(it.id, Boolean(it.isFollowed))}
                                >
                                    {it.isFollowed ? 'Unfollow' : 'Follow back'}
                                </Button>
                            )}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
