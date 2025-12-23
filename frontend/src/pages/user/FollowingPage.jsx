import React, { useEffect, useState, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import { getFollowingApi, followUserApi, unfollowUserApi } from '../../api/user-api.jsx';
import './style/FollowingPage.css';

export default function FollowingPage() {
    const { userId } = useParams();
    const navigate = useNavigate();
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);

    const [loading, setLoading] = useState(true);
    const [items, setItems] = useState([]);
    const [error, setError] = useState(null);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setError(null);
        getFollowingApi({ userId: Number(userId), page: 0, size: 100 }).then(res => {
            if (cancelled) return;
            if (res.success && Array.isArray(res.data)) {
                setItems(res.data);
            } else {
                setError(res.message || 'Following list is not available');
            }
        }).catch(err => {
            if (!cancelled) setError(err?.message || 'Failed to load following');
        }).finally(() => { if (!cancelled) setLoading(false); });

        return () => { cancelled = true; };
    }, [userId]);

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
                                    <div className="follower-avatar">{it.avatar ? <img src={it.avatar} alt="avatar" /> : <span className="avatar-fallback">{(it.username||'U').charAt(0).toUpperCase()}</span>}</div>
                                    <div className="follower-meta">
                                        <div className="follower-name">{it.username || it.displayName || `User ${it.id}`}</div>
                                        <div className="follower-sub">{it.bio || ''}</div>
                                    </div>
                                    <div className="follower-actions">
                                        <button
                                            className={`btn btn-fill follow-btn ${it.isFollowed ? 'following' : ''}`}
                                            onClick={() => handleFollowToggle(it.id, Boolean(it.isFollowed))}
                                        >
                                            {it.isFollowed ? 'Following' : 'Follow'}
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
