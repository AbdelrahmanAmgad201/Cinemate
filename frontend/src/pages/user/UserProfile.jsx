import { useParams } from 'react-router-dom';
import { useEffect, useState, useContext, useRef } from 'react';
import { AuthContext } from '../../context/AuthContext.jsx';
import { IoIosPerson } from 'react-icons/io';
import { FaUserPlus, FaUserCheck } from 'react-icons/fa';
import { ToastContext } from '../../context/ToastContext.jsx';
import { getModApi } from '../../api/forum-api.jsx';
import { getUserProfileApi, getUserIsPublicApi, isUserFollowedApi, followUserApi, unfollowUserApi } from '../../api/user-api.jsx';
import { formatCount } from '../../utils/formate.jsx';
import './style/UserProfile.css';
import UserProfileSidebar from '../../components/UserProfileSidebar.jsx';
import PersonalData from '../../components/PersonalData.jsx';

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

    useEffect(() => {
        setLoading(true);
    }, [userId]);

    const [fetchedName, setFetchedName] = useState(null);
    const [profile, setProfile] = useState(null);
    const { showToast } = useContext(ToastContext);
    const [isFollowing, setIsFollowing] = useState(false);
    const [followBusy, setFollowBusy] = useState(false);
    const desiredFollowRef = useRef(null);

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
                if (!ignore && res?.success && res.data) {
                    const text = res.data;
                    if (text && text !== 'Unknown user') {
                        setFetchedName(text);
                    }
                }
            }).catch(e => {});
            setLoading(false);
        }
        else if (isNumericId) {
            setLoading(true);
            getUserProfileApi({ userId: Number(userId) }).then(res => {
                if (!ignore && res?.success && res.data) {
                    setProfile(res.data);
                    const name = `${res.data.firstName || ''} ${res.data.lastName || ''}`.trim();
                    if (name) setFetchedName(name);
                } else if (!ignore) {
                    showToast('Failed to load profile', res?.message || 'Unknown error', 'error');
                }
            }).catch(e => {
                console.error(e);
                if (!ignore) showToast('Failed to load profile', 'Unknown error', 'error');
            }).finally(() => { if (!ignore) setLoading(false); });
        }
        else {
            setLoading(false);
        }

        return () => { ignore = true; };
    }, [userId]);
    const isOwnProfile = user && (String(user.id) === String(userId));
    const visibleTabs = isOwnProfile
        ? TABS.filter(t => t.key !== 'personal')
        : TABS.filter(t => ['posts','forums','liked','reviews'].includes(t.key));

    useEffect(() => {
        if (active === 'personal') return;
        if (!visibleTabs.find(t => t.key === active)) {
            setActive(visibleTabs[0]?.key || 'posts');
        }
    }, [visibleTabs, userId, active]);

    const tabListRef = useRef(null);
    const headerRef = useRef(null);
    const sidebarRef = useRef(null);
    const [showLeftArrow, setShowLeftArrow] = useState(false);
    const [showRightArrow, setShowRightArrow] = useState(false);
    const leftBtnRef = useRef(null);
    const rightBtnRef = useRef(null);
    const [showProfileSidebar, setShowProfileSidebar] = useState(true);
    const checkSidebarOverlap = () => {
        const leftNav = document.querySelector('.user-left-sidebar');
        const sidebar = sidebarRef.current;
        if (!leftNav || !sidebar || window.innerWidth <= 768) {
            setShowProfileSidebar(true);
            return;
        }
        const leftRect = leftNav.getBoundingClientRect();
        const sideRect = sidebar.getBoundingClientRect();
        const minMargin = 16;
        const minMainWidth = 520;
        const sidebarWidthFallback = 370;
        const sidebarWidth = sideRect.width || sidebarWidthFallback;
        const availableBetween = (window.innerWidth - leftRect.right - sidebarWidth);
        const visible = (availableBetween > (minMainWidth + minMargin));
        if (typeof process !== 'undefined' && process.env && process.env.NODE_ENV === 'development') {
            console.debug('[ProfileOverlap] leftRight=', leftRect.right, 'sideRectLeft=', sideRect.left, 'sideWidth=', sidebarWidth, 'availableBetween=', availableBetween, 'visible=', visible);
        }
        if (showProfileSidebar !== visible) setShowProfileSidebar(visible);
    };

    const updateTabOverflow = () => {
        const list = tabListRef.current;
        const header = headerRef.current;
        const sidebar = sidebarRef.current;
        const leftBtn = leftBtnRef.current;
        const rightBtn = rightBtnRef.current;
        if (!list || !header) return;
        const isNarrow = window.innerWidth <= 768;
        checkSidebarOverlap();
        const sidebarWidth = (!isNarrow && sidebar && showProfileSidebar) ? Math.max(0, sidebar.getBoundingClientRect().width) : 0;
        const available = Math.max(80, header.getBoundingClientRect().width - sidebarWidth - 24);
        list.style.maxWidth = `${available}px`;
        const leftVisible = list.scrollLeft > 0;
        const rightVisible = list.scrollWidth > list.clientWidth + list.scrollLeft + 1;
        setShowLeftArrow(leftVisible);
        setShowRightArrow(rightVisible);
        if (rightVisible && rightBtn) {
            const headerRect = header.getBoundingClientRect();
            const listRect = list.getBoundingClientRect();
            const btnRect = rightBtn.getBoundingClientRect();
            let left = Math.round(listRect.right - headerRect.left - btnRect.width - 8);
            if (left + btnRect.width > headerRect.width - 8) left = headerRect.width - btnRect.width - 8;
            if (left < 8) left = 8;
            rightBtn.style.left = `${left}px`;
        }

        if (leftVisible && leftBtn) {
            const headerRect = header.getBoundingClientRect();
            const listRect = list.getBoundingClientRect();
            const btnRect = leftBtn.getBoundingClientRect();
            let left = Math.round(listRect.left - headerRect.left + 8);
            if (left < 8) left = 8;
            leftBtn.style.left = `${left}px`;
        }
    };

    useEffect(() => {
        updateTabOverflow();
        const onResize = () => { checkSidebarOverlap(); updateTabOverflow(); };
        window.addEventListener('resize', onResize);
        window.addEventListener('focus', onResize);
        const leftNav = document.querySelector('.user-left-sidebar');
        let observer = null;
        if (leftNav && window.MutationObserver) {
            observer = new MutationObserver(() => { checkSidebarOverlap(); updateTabOverflow(); });
            observer.observe(leftNav, { attributes: true, attributeFilter: ['class'] });
        }
        return () => {
            window.removeEventListener('resize', onResize);
            window.removeEventListener('focus', onResize);
            if (observer) observer.disconnect();
        };
    }, [visibleTabs]);

    useEffect(() => { updateTabOverflow(); }, [showProfileSidebar]);

    const pollRef = useRef(null);
    useEffect(() => {
        if (!showProfileSidebar) {
            if (!pollRef.current) {
                pollRef.current = setInterval(() => { checkSidebarOverlap(); }, 300);
            }
        } else {
            if (pollRef.current) { clearInterval(pollRef.current); pollRef.current = null; }
        }
        return () => { if (pollRef.current) { clearInterval(pollRef.current); pollRef.current = null; } };
    }, [showProfileSidebar]);

    const formatAccountAge = (created) => {
        if (!created) return '—';
        const date = new Date(created);
        if (Number.isNaN(date.getTime())) return '—';
        const msDiff = Date.now() - date.getTime();
        const days = Math.max(0, Math.floor(msDiff / (1000 * 60 * 60 * 24)));
        const years = Math.floor(days / 365);
        const months = Math.floor((days % 365) / 30);
        if (years > 0) return months > 0 ? `${years}y ${months}m` : `${years}y`;
        if (months > 0) return `${months}m`;
        return `${days}d`;
    };

    const handleTabScroll = () => {
        const list = tabListRef.current;
        if (!list) return;
        setShowLeftArrow(list.scrollLeft > 0);
        setShowRightArrow(list.scrollWidth > list.clientWidth + list.scrollLeft + 1);
        updateTabOverflow();
    };

    const scrollRight = () => {
        const list = tabListRef.current;
        if (!list) return;
        list.scrollBy({ left: Math.round(list.clientWidth * 0.65), behavior: 'smooth' });
        setTimeout(handleTabScroll, 300);
    };

    const scrollLeft = () => {
        const list = tabListRef.current;
        if (!list) return;
        list.scrollBy({ left: -Math.round(list.clientWidth * 0.65), behavior: 'smooth' });
        setTimeout(handleTabScroll, 300);
    };



    useEffect(() => {
        let cancelled = false;
        if (isOwnProfile) {
            return () => { cancelled = true; };
        }
        if (!user) {
            return () => { cancelled = true; };
        }
        if (!userId) {
            return () => { cancelled = true; };
        }
        if (!/^\d+$/.test(String(userId))) {
            return () => { cancelled = true; };
        }
        isUserFollowedApi({ userId: Number(userId) })
            .then(res => { if (!cancelled && res.success) setIsFollowing(Boolean(res.data)); })
            .catch(() => {});
        return () => { cancelled = true; };
    }, [userId, isOwnProfile, user]);

    const applyFollowDesired = async (desired) => {
        setFollowBusy(true);

        const idNum = Number(userId);
        const res = desired ? await followUserApi({ userId: idNum }) : await unfollowUserApi({ userId: idNum });

        if (!res?.success) {
            showToast('Follow action failed', res?.message || 'Please try again', 'error');
        } else {
            setIsFollowing(desired);
            showToast(desired ? 'Following' : 'Unfollowed', desired ? 'You are now following this user' : 'Follow removed', 'success');
        }

        const pending = desiredFollowRef.current;
        if (pending !== null && pending !== desired) {
            desiredFollowRef.current = null;
            await applyFollowDesired(pending);
            return;
        } else {
            desiredFollowRef.current = null;
            const prof = await getUserProfileApi({ userId: Number(userId) });
            if (prof?.success && prof.data) setProfile(prof.data);
            setFollowBusy(false);
        }
    };

    const handleFollowToggle = () => {
        const nextDesired = !isFollowing;
        desiredFollowRef.current = nextDesired;
        if (!followBusy) {
            applyFollowDesired(nextDesired);
        }
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
                            <span className="user-name">{displayName}</span>

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

    
                    </div>
                </div>

                <div className="profile-tabbar" role="tablist" ref={headerRef}>
                    <hr className="profile-header-sep" />
                    <div className="tab-scroll-wrap">
                        <div className="tab-list" ref={tabListRef} onScroll={handleTabScroll}>
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

                        {showLeftArrow && (<button ref={leftBtnRef} className="tab-scroll-btn left" onClick={scrollLeft} aria-label="Scroll left">‹</button>)}

                        {showRightArrow && (<button ref={rightBtnRef} className="tab-scroll-btn right" onClick={scrollRight} aria-label="Scroll right">›</button>)}
                    </div>
                </div>
            </div>

            <div className={`profile-main-grid ${!showProfileSidebar ? 'sidebar-hidden' : ''}`}>
                <main className="profile-content-col">
                    <div className="profile-section">
                        <div className="section-title">{TABS.find(t => t.key === active).label}</div>

                        {active === 'personal' && (
                            <PersonalData profile={profile} user={user} />
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
                </main>

                <UserProfileSidebar
                    sidebarRef={sidebarRef}
                    showProfileSidebar={showProfileSidebar}
                    displayName={displayName}
                    user={user}
                    profile={profile}
                    isOwnProfile={isOwnProfile}
                    setActive={setActive}
                    avatarSrc={avatarSrc}
                    formatAccountAge={formatAccountAge}
                    followersCount={followersCount}
                    followingCount={followingCount}
                />
            </div>
        </div>
    );
}