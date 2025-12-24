import React, { useEffect, useState, useContext } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { getFollowingApi, followUserApi, unfollowUserApi } from '../../api/user-api.jsx';
import { PATHS } from '../../constants/constants.jsx';
import { IoIosPerson } from 'react-icons/io';
import './style/FollowingPage.css';

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
            showToast('Access Denied', 'You can only view your own following list.', 'warning');
            navigate(-1);
            return;
        }

        let cancelled = false;
        setLoading(true);
        setError(null);
        
        getFollowingApi({ page: 0, size: 100 }).then(res => {
            if (cancelled) return;
            if (res.success && res.data) {
                const followings = res.data.content || [];
                
                const mappedFollowings = followings.map((following) => {
                    const followedUser = following.followedUser;
                    if (!followedUser) return null;
                    
                    return {
                        id: followedUser.id,
                        firstName: followedUser.firstName || '',
                        lastName: followedUser.lastName || '',
                        username: `${followedUser.firstName || ''} ${followedUser.lastName || ''}`.trim() || `User ${followedUser.id}`,
                        isFollowed: true
                    };
                }).filter(item => item !== null);
                
                setItems(mappedFollowings);
            } else {
                setError(res.message || 'Following list is not available');
            }
        }).catch(err => {
            if (!cancelled) setError(err?.message || 'Failed to load following');
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
        <div className="following-page">
            <div className="following-container">
                <div className="following-header">
                    <button 
                        className="back-button" 
                        onClick={() => navigate(-1)}
                        aria-label="Go back"
                    >
                        ← Back
                    </button>
                    <h1>Following</h1>
                </div>
                
                <div className="following-content">
                    {loading && <p className="placeholder-text">Loading following…</p>}
                    {!loading && error && (
                        <div className="placeholder-text">{error}</div>
                    )}

                    {!loading && !error && items.length === 0 && (
                        <div className="placeholder-text">No following yet.</div>
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
                                        <button
                                            className={`btn btn-fill follow-btn ${it.isFollowed ? 'following' : ''}`}
                                            onClick={() => handleFollowToggle(it.id, Boolean(it.isFollowed))}
                                        >
                                            {it.isFollowed ? 'Unfollow' : 'Follow'}
                                        </button>
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
