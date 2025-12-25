import React, { useEffect, useState, useContext } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { getFollowersApi, followUserApi, unfollowUserApi, isUserFollowedApi } from '../../api/user-api.jsx';
import { PATHS } from '../../constants/constants.jsx';
import { IoIosPerson } from 'react-icons/io';
import './style/FollowersPage.css';

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
            showToast('Access Denied', 'You can only view your own followers list.', 'warning');
            navigate(-1);
            return;
        }

        let cancelled = false;
        setLoading(true);
        setError(null);
        
        getFollowersApi({ page: 0, size: 100 }).then(async res => {
            if (cancelled) return;
            if (res.success && res.data) {
                const followers = res.data.content || [];
                
                const mappedFollowers = await Promise.all(followers.map(async (follower) => {
                    const followingUser = follower.followingUser;
                    if (!followingUser) return null;
                    
                    const followStatus = await isUserFollowedApi({ userId: followingUser.id });
                    
                    return {
                        id: followingUser.id,
                        firstName: followingUser.firstName || '',
                        lastName: followingUser.lastName || '',
                        username: `${followingUser.firstName || ''} ${followingUser.lastName || ''}`.trim() || `User ${followingUser.id}`,
                        isFollowed: followStatus.success ? followStatus.data : false
                    };
                }));
                
                setItems(mappedFollowers.filter(item => item !== null));
            } else {
                setError(res.message || 'Followers list is not available');
            }
        }).catch(err => {
            if (!cancelled) setError(err?.message || 'Failed to load followers');
        }).finally(() => { if (!cancelled) setLoading(false); });

        return () => { cancelled = true; };
    }, [userId, user, isOwnProfile, navigate, showToast]);

    const handleFollowToggle = async (targetId, currentlyFollowing) => {
        if (!user) {
            showToast('Sign in required', 'Please sign in to follow users.', 'info');
            return;
        }

        try {
            const res = currentlyFollowing ? await unfollowUserApi({ userId: targetId }) : await followUserApi({ userId: targetId });
            if (!res.success) throw new Error(res.message || 'Action failed');
            setItems(prev => prev.map(it => it.id === targetId ? ({ ...it, isFollowed: !currentlyFollowing }) : it));
        } catch (err) {
            showToast('Error', err?.message || 'Failed to perform follow action', 'error');
        }
    };

    return (
        <div className="followers-page">
            <div className="followers-container">
                <div className="followers-header">
                    <button 
                        className="back-button" 
                        onClick={() => navigate(-1)}
                        aria-label="Go back"
                    >
                        ← Back
                    </button>
                    <h1>Followers</h1>
                </div>
                
                <div className="followers-content">
                    {loading && <p className="placeholder-text">Loading followers…</p>}
                    {!loading && error && (
                        <div className="placeholder-text">{error}</div>
                    )}

                    {!loading && !error && items.length === 0 && (
                        <div className="placeholder-text">No followers yet.</div>
                    )}

                    {!loading && !error && items.length > 0 && (
                        <div className="followers-list">
                            {items.map(it => (
                                <div key={it.id} className="follower-item">
                                    <Link to={PATHS.USER.PROFILE(it.id)} className="follower-avatar-link">
                                        <div className="follower-avatar">
                                            {it.avatar ? (
                                                <img src={it.avatar} alt={it.username} />
                                            ) : (
                                                <IoIosPerson className="avatar-fallback-icon" />
                                            )}
                                        </div>
                                    </Link>
                                    <div className="follower-meta">
                                        <Link to={PATHS.USER.PROFILE(it.id)} className="follower-name-link">
                                            <div className="follower-name">{it.username}</div>
                                        </Link>
                                    </div>
                                    <div className="follower-actions">
                                        {it.id !== user?.id && (
                                            <button
                                                className={`btn btn-fill follow-btn ${it.isFollowed ? 'following' : ''}`}
                                                onClick={() => handleFollowToggle(it.id, Boolean(it.isFollowed))}
                                                aria-pressed={Boolean(it.isFollowed)}
                                                title={it.isFollowed ? 'Unfollow' : 'Follow back'}
                                            >
                                                {it.isFollowed ? 'Unfollow' : 'Follow back'}
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
