import { useParams } from 'react-router-dom';
import { useEffect, useState, useContext, useRef } from 'react';
import { AuthContext } from '../../context/AuthContext.jsx';
import { IoIosPerson } from 'react-icons/io';
import { FaUserPlus, FaUserCheck } from 'react-icons/fa';
import { ToastContext } from '../../context/ToastContext.jsx';
import { getModApi } from '../../api/forum-api.jsx';
import './style/UserProfile.css';

const TABS = [
    { key: 'personal', label: 'Personal data' },
    { key: 'posts', label: 'Posts' },
    { key: 'forums', label: 'Forums' },
    { key: 'history', label: 'Watch history' },
    { key: 'watchlater', label: 'Watch later' },
    { key: 'liked', label: 'Liked movies' },
    { key: 'reviews', label: 'Movie reviews' },
];

export default function UserProfile() {
    const { userId } = useParams();
    const { user } = useContext(AuthContext);
    const [loading, setLoading] = useState(true);
    const [active, setActive] = useState(TABS[0].key);

    useEffect(() => {
        // Load basic info if available (use AuthContext for current user)
        setLoading(false);
    }, [userId]);

    const [fetchedName, setFetchedName] = useState(null);

    const { showToast } = useContext(ToastContext);

    const [isFollowing, setIsFollowing] = useState(false); // local optimistic follow state
    const followLastToggleAtRef = useRef(0);

    const displayName = (() => {
        // If viewing own profile, prefer authenticated user data
        if (user && String(user.id) === String(userId)) {
            return `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email;
        }
        // If we fetched a name for an object id, use it
        if (fetchedName) return fetchedName;
        // fallback
        return `User ${userId}`;
    })();

    useEffect(() => {
        // If the param looks like a 24-char Mongo ObjectId, try fetching a display name
        let ignore = false;
        const isObjectId = typeof userId === 'string' && /^[0-9a-fA-F]{24}$/.test(userId);
        if (isObjectId) {
            getModApi({ userId }).then(res => {
                if (!ignore && res?.success && res.data) {
                    const text = res.data;
                    if (text && text !== 'Unknown user') {
                        setFetchedName(text);
                    }
                }
            }).catch(e => {});
        }
        return () => { ignore = true; };
    }, [userId]);

    // Is the profile being viewed by the owner?
    const isOwnProfile = user && (String(user.id) === String(userId));

    // Tabs visible depends on owner vs other user
    const visibleTabs = isOwnProfile
        ? TABS
        : TABS.filter(t => ['posts','forums'].includes(t.key));

    // Ensure active tab is valid for current visibility
    useEffect(() => {
        if (!visibleTabs.find(t => t.key === active)) {
            setActive(visibleTabs[0]?.key || 'posts');
        }
    }, [visibleTabs, userId]);

    const handleFollowToggle = () => {
        const now = Date.now();
        // guard against double clicks / duplicate events within 400ms
        if (now - followLastToggleAtRef.current < 400) {
            return;
        }
        followLastToggleAtRef.current = now;

        if (!user) {
            showToast('Sign in required', 'Please sign in to follow users.', 'info');
            return;
        }

        // Toggle locally — backend will be implemented later
        const next = !isFollowing;
        setIsFollowing(next);
        showToast(next ? 'Following' : 'Unfollowed', next ? 'You are now following this user' : 'Follow removed', 'success');
    };

    if (loading) return <div>Loading...</div>;

    const avatarSrc = (user && String(user.id) === String(userId) && user.avatar) ? user.avatar : null;

    return (
        <div className="user-profile-container">
            <div className="user-profile-header">
                <div className="header-top">
                    <div className="profile-avatar-circle" aria-hidden>
                        {avatarSrc ? <img src={avatarSrc} alt="avatar" /> : <IoIosPerson size={36} />}
                    </div>
                    <div className="name-and-meta">
                        <div className="name-row">
                            <div className="user-name">{displayName}</div>

                            {!isOwnProfile && (
                                <button
                                    type="button"
                                    className={`btn btn-fill follow-btn ${isFollowing ? 'following' : ''}`}
                                    onClick={handleFollowToggle}
                                    aria-pressed={isFollowing}
                                >
                                    {isFollowing ? <><FaUserCheck /> Following</> : <><FaUserPlus /> Follow</>}
                                </button>
                            )}
                        </div>

                        <div className="user-meta">{String(userId)}</div>
                    </div>
                </div>

                <div className="profile-tabbar" role="tablist">
                    {visibleTabs.map(tab => (
                        <button
                            key={tab.key}
                            className={`profile-tab ${active === tab.key ? 'active' : ''}`}
                            onClick={() => setActive(tab.key)}
                            role="tab"
                            aria-selected={active === tab.key}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>
            </div>

            <div className="profile-section">
                <div className="section-title">{TABS.find(t => t.key === active).label}</div>

                {active === 'personal' && (
                    <div>
                        <p className="placeholder-note">Personal data read endpoint not implemented yet.</p>
                        <p><strong>Email:</strong> {user && user.email ? user.email : '-'}</p>
                        <p><strong>Name:</strong> {user && (user.firstName || user.lastName) ? `${user.firstName || ''} ${user.lastName || ''}` : '-'}</p>
                    </div>
                )}

                {active === 'history' && (
                    <div>
                        <p className="placeholder-note">Watch history endpoint missing — will display a paged list of movies when implemented.</p>
                    </div>
                )}

                {active === 'watchlater' && (
                    <div>
                        <p className="placeholder-note">Watch later endpoint missing — will list saved movies when implemented.</p>
                    </div>
                )}

                {active === 'liked' && (
                    <div>
                        <p className="placeholder-note">Liked movies endpoint missing — will show movie thumbnails or titles when implemented.</p>
                    </div>
                )}

                {active === 'reviews' && (
                    <div>
                        <p className="placeholder-note">Reviews by this user are not yet queryable via the API.</p>
                    </div>
                )}

                {active === 'forums' && (
                    <div>
                        <p className="placeholder-note">Forums owned by this user: endpoint missing. Will list forums with links.</p>
                    </div>
                )}

                {active === 'posts' && (
                    <div>
                        <p className="placeholder-note">Posts authored by this user: endpoint missing. Will show paged posts when implemented.</p>
                    </div>
                )}
            </div>
        </div>
    );
}