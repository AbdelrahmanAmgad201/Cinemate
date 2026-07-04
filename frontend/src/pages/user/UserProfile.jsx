import { useParams } from 'react-router-dom';
import { useEffect, useState, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext.jsx';
import { UserPlus, UserCheck, Lock } from 'lucide-react';
import { ToastContext } from '../../context/ToastContext.jsx';
import { getModApi } from '../../api/forum-api.js';
import UserReviews from '../../components/UserReviews.jsx';
import OwnedForums from '../../components/OwnedForums.jsx';
import WatchHistory from '../../components/WatchHistory.jsx';
import UserPosts from '../../components/UserPosts.jsx';
import { getUserProfileApi, isUserFollowedApi, followUserApi, unfollowUserApi } from '../../api/user-api.js';
import WatchLaterPanel from '../../components/WatchLaterPanel.jsx';

import './style/UserProfile.css';
import UserProfileSidebar from '../../components/UserProfileSidebar.jsx';
import LikedMoviesPanel from '../../components/LikedMoviesPanel.jsx';
import ScrollToTop from '../../components/ScrollToTop.jsx';
import PersonalData from '../../components/PersonalData.jsx';
import Avatar from '../../components/ui/Avatar.jsx';
import Badge from '../../components/ui/Badge.jsx';
import Button from '../../components/ui/Button.jsx';
import Tabs from '../../components/ui/Tabs.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';
import LoadingFallback from '../../components/LoadingFallback.jsx';

const TABS = [
    { key: 'posts', label: 'Posts' },
    { key: 'forums', label: 'Forums' },
    { key: 'watchlater', label: 'Watch Later' },
    { key: 'history', label: 'Watch History' },
    { key: 'liked', label: 'Liked Movies' },
    { key: 'reviews', label: 'Reviews' },
    { key: 'personal', label: 'Personal data' },
];

export default function UserProfile() {
    const { userId } = useParams();
    const { user } = useContext(AuthContext);
    const [loading, setLoading] = useState(true);
    const [active, setActive] = useState(TABS[0].key);

    const [fetchedName, setFetchedName] = useState(null);
    const [profile, setProfile] = useState(null);
    const [isPrivateProfile, setIsPrivateProfile] = useState(false);
    const { showToast } = useContext(ToastContext);
    const [isFollowing, setIsFollowing] = useState(false);
    const [followBusy, setFollowBusy] = useState(false);

    const followersCount = profile?.numberOfFollowers ?? user?.followersCount ?? user?.followers ?? 0;
    const followingCount = profile?.numberOfFollowing ?? user?.followingCount ?? user?.following ?? 0;

    const displayName = (() => {
        if (profile && (profile.firstName || profile.lastName)) {
            return `${profile.firstName || ''} ${profile.lastName || ''}`.trim();
        }
        if (user && String(user.id) === String(userId)) {
            return user.email || `User ${userId}`;
        }
        if (fetchedName) return fetchedName;
        return `User ${userId}`;
    })();

    useEffect(() => {
        let ignore = false;
        const isObjectId = typeof userId === 'string' && /^[0-9a-fA-F]{24}$/.test(userId);
        const isNumericId = typeof userId === 'string' && /^\d+$/.test(userId);

        if (isObjectId) {
            getModApi({ userId }).then(res => {
                if (!ignore && res?.success && res.data && res.data !== 'Unknown user') {
                    setFetchedName(res.data);
                }
            }).catch(() => {});
            setLoading(false);
        } else if (isNumericId) {
            setLoading(true);
            getUserProfileApi({ userId: Number(userId) }).then(res => {
                if (ignore) return;
                if (res?.success && res.data) {
                    setProfile(res.data);
                    setIsPrivateProfile(false);
                    const name = `${res.data.firstName || ''} ${res.data.lastName || ''}`.trim();
                    if (name) setFetchedName(name);
                } else {
                    const msg = res?.message || '';
                    if (res?.status === 403 || /profile is private|this profile is private|getIsPublic/i.test(String(msg))) {
                        setIsPrivateProfile(true);
                    } else {
                        showToast('Failed to load profile', msg || 'Unknown error', 'error');
                    }
                }
            }).finally(() => { if (!ignore) setLoading(false); });
        } else {
            setLoading(false);
        }

        return () => { ignore = true; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [userId]);

    const isOwnProfile = user && (String(user.id) === String(userId));
    const visibleTabs = isOwnProfile
        ? TABS.filter(t => t.key !== 'personal')
        : TABS.filter(t => ['posts', 'liked', 'reviews'].includes(t.key));

    useEffect(() => {
        if (active === 'personal') return;
        if (!visibleTabs.find(t => t.key === active)) {
            setActive(visibleTabs[0]?.key || 'posts');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [visibleTabs, userId]);

    const formatAccountAge = (created) => {
        if (!created) return '—';
        const date = new Date(created);
        if (Number.isNaN(date.getTime())) return '—';
        const days = Math.max(0, Math.floor((Date.now() - date.getTime()) / (1000 * 60 * 60 * 24)));
        const years = Math.floor(days / 365);
        const months = Math.floor((days % 365) / 30);
        if (years > 0) return months > 0 ? `${years}y ${months}m` : `${years}y`;
        if (months > 0) return `${months}m`;
        return `${days}d`;
    };

    useEffect(() => {
        let cancelled = false;
        if (isOwnProfile || !user || !userId || !/^\d+$/.test(String(userId))) return;

        isUserFollowedApi({ userId: Number(userId) })
            .then(res => { if (!cancelled && res.success) setIsFollowing(Boolean(res.data)); })
            .catch(() => {});
        return () => { cancelled = true; };
    }, [userId, isOwnProfile, user]);

    const handleFollowToggle = async () => {
        if (followBusy) return;
        setFollowBusy(true);
        const desired = !isFollowing;
        const idNum = Number(userId);
        const res = desired ? await followUserApi({ userId: idNum }) : await unfollowUserApi({ userId: idNum });

        if (!res?.success) {
            showToast('Follow action failed', res?.message || 'Please try again', 'error');
        } else {
            setIsFollowing(desired);
            showToast(desired ? 'Following' : 'Unfollowed', desired ? 'You are now following this user' : 'Follow removed', 'success');
            const prof = await getUserProfileApi({ userId: idNum });
            if (prof?.success && prof.data) setProfile(prof.data);
        }
        setFollowBusy(false);
    };

    if (loading) return <LoadingFallback fullScreen />;

    const avatarSrc = (user && String(user.id) === String(userId) && user.avatar) ? user.avatar : null;

    return (
        <div className="user-profile-container">
            <div className="user-profile-header">
                <div className="header-top">
                    <Avatar name={displayName} src={avatarSrc} size="xl" />
                    <div className="name-and-meta">
                        <div className="name-row">
                            <span className="user-name">{displayName}</span>
                            {isPrivateProfile && !isOwnProfile && (
                                <Badge variant="neutral"><Lock size={12} /> Private profile</Badge>
                            )}
                            {!isOwnProfile && (
                                <Button
                                    size="sm"
                                    variant={isFollowing ? 'secondary' : 'primary'}
                                    icon={isFollowing ? <UserCheck size={15} /> : <UserPlus size={15} />}
                                    onClick={handleFollowToggle}
                                    loading={followBusy}
                                >
                                    {isFollowing ? 'Following' : 'Follow'}
                                </Button>
                            )}
                        </div>
                    </div>
                </div>

                <Tabs tabs={visibleTabs} activeId={active} onChange={setActive} className="profile-tabbar" />
            </div>

            <div className="profile-main-grid">
                <main className="profile-content-col">
                    <div className="profile-section">
                        {active === 'personal' && <PersonalData profile={profile} user={user} />}
                        {active === 'history' && <WatchHistory active={active} isOwnProfile={isOwnProfile} />}
                        {active === 'watchlater' && <WatchLaterPanel />}
                        {active === 'liked' && <LikedMoviesPanel userId={Number(userId)} my={isOwnProfile} />}
                        {active === 'reviews' && <UserReviews userId={userId} profile={profile} isOwnProfile={isOwnProfile} />}
                        {active === 'forums' && (
                            isOwnProfile
                                ? <OwnedForums />
                                : <EmptyState title="Forums owned by this user" description="This view isn't available yet." />
                        )}
                        {active === 'posts' && (
                            isPrivateProfile && !isOwnProfile
                                ? <EmptyState icon={<Lock size={28} />} title="This profile is private" description="Posts and personal info aren't visible." />
                                : <UserPosts userId={userId} isOwnProfile={isOwnProfile} active={active} />
                        )}
                    </div>
                </main>

                <UserProfileSidebar
                    displayName={displayName}
                    user={user}
                    profile={profile}
                    isOwnProfile={isOwnProfile}
                    setActive={setActive}
                    avatarSrc={avatarSrc}
                    formatAccountAge={formatAccountAge}
                    followersCount={followersCount}
                    followingCount={followingCount}
                    userId={userId}
                />
            </div>
            <ScrollToTop />
        </div>
    );
}
