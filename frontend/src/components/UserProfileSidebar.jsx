import { useState, useRef, useEffect, useContext } from 'react';
import { Link } from 'react-router-dom';
import { Settings, Globe, Lock } from 'lucide-react';
import { PATHS } from '../constants/constants.jsx';
import './style/UserProfileSidebar.css';
import { getIsPublicApi, setIsPublicApi } from '../api/user-api.js';
import { ToastContext } from '../context/ToastContext.jsx';
import { formatCount } from '../utils/formate.jsx';
import Avatar from './ui/Avatar.jsx';

function AboutBlock({ user, profile }) {
    const [aboutExpanded, setAboutExpanded] = useState(false);
    const [aboutOverflow, setAboutOverflow] = useState(false);
    const aboutRef = useRef(null);
    const aboutText = (profile?.aboutMe?.trim()) || (user?.about?.trim()) || 'No info about the user';

    useEffect(() => {
        const el = aboutRef.current;
        if (!el) return;
        const check = () => setAboutOverflow(el.scrollHeight > el.clientHeight + 1);
        check();
        window.addEventListener('resize', check);
        return () => window.removeEventListener('resize', check);
    }, [aboutText]);

    const id = `about-${user?.id || 'profile'}`;

    return (
        <div>
            <p id={id} ref={aboutRef} className={`about-text ${aboutExpanded ? 'expanded' : 'collapsed'}`}>
                {aboutText}
            </p>

            {aboutText !== 'No info about the user' && (aboutOverflow || aboutExpanded) && (
                <button
                    type="button"
                    className="about-toggle"
                    onClick={() => setAboutExpanded((v) => !v)}
                    aria-expanded={aboutExpanded}
                    aria-controls={id}
                >
                    {aboutExpanded ? 'Show less' : 'Show more'}
                </button>
            )}
        </div>
    );
}

function PrivacyToggle() {
    const { showToast } = useContext(ToastContext);
    const [isPublic, setIsPublic] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        let mounted = true;
        getIsPublicApi().then((res) => {
            if (!mounted) return;
            if (res.success) setIsPublic(Boolean(res.data));
            else showToast('Failed to load visibility', res.message || 'Unknown error', 'error');
        }).finally(() => { if (mounted) setLoading(false); });
        return () => { mounted = false; };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleToggle = async () => {
        if (loading || saving) return;
        const next = !isPublic;
        setIsPublic(next);
        setSaving(true);

        const res = await setIsPublicApi({ isPublic: next });
        if (!res.success) {
            setIsPublic(!next);
            showToast('Failed to update visibility', res.message || 'Unknown error', 'error');
        } else {
            showToast('Saved', next ? 'Profile is public' : 'Profile is private', 'success');
        }
        setSaving(false);
    };

    return (
        <button type="button" className="settings-row" onClick={handleToggle} disabled={loading || saving} aria-pressed={!!isPublic}>
            <span className="settings-row__icon">{isPublic ? <Globe size={16} /> : <Lock size={16} />}</span>
            <span className="settings-row__text">
                <span className="settings-row__title">Profile visibility</span>
                <span className="settings-row__sub">
                    {loading ? 'Loading…' : isPublic ? 'Anyone can view your profile' : 'Only you can view your profile'}
                </span>
            </span>
            <span className={`toggle-switch ${isPublic ? 'toggle-switch--on' : ''}`} aria-hidden="true">
                <span className="toggle-switch__thumb" />
            </span>
        </button>
    );
}

export default function UserProfileSidebar({
    displayName,
    user,
    profile,
    isOwnProfile,
    setActive,
    avatarSrc,
    formatAccountAge,
    followersCount = 0,
    followingCount = 0,
    userId,
}) {
    const id = user?.id || profile?.id || userId;
    const followersLink = PATHS.USER.FOLLOWERS(id);
    const followingLink = PATHS.USER.FOLLOWING(id);

    return (
        <aside className="profile-sidebar">
            <div className="sidebar-card">
                <Avatar name={displayName} src={avatarSrc} size="lg" />
                <h2 className="sidebar-title">{displayName}</h2>

                <div className="sidebar-stats-inline">
                    {isOwnProfile ? (
                        <Link to={followersLink} className="stat-inline stat-inline-clickable">
                            <span className="stat-num-inline">{formatCount(followersCount)}</span>
                            <span className="stat-label-inline">followers</span>
                        </Link>
                    ) : (
                        <div className="stat-inline">
                            <span className="stat-num-inline">{formatCount(followersCount)}</span>
                            <span className="stat-label-inline">followers</span>
                        </div>
                    )}
                    {isOwnProfile ? (
                        <Link to={followingLink} className="stat-inline stat-inline-clickable">
                            <span className="stat-num-inline">{formatCount(followingCount)}</span>
                            <span className="stat-label-inline">following</span>
                        </Link>
                    ) : (
                        <div className="stat-inline">
                            <span className="stat-num-inline">{formatCount(followingCount)}</span>
                            <span className="stat-label-inline">following</span>
                        </div>
                    )}
                </div>

                <div className="sidebar-about">
                    <div className="about-title">About</div>
                    <AboutBlock user={user} profile={profile} />
                </div>

                <div className="sidebar-account-age">
                    <span className="stat-num">{formatAccountAge(profile?.createdAt || user?.createdAt)}</span>
                    <span className="stat-label">account age</span>
                </div>

                {isOwnProfile && (
                    <>
                        <hr className="sidebar-divider" />
                        <div className="sidebar-settings">
                            <h3>Settings</h3>
                            <button type="button" className="settings-row" onClick={() => setActive?.('personal')}>
                                <span className="settings-row__icon"><Settings size={16} /></span>
                                <span className="settings-row__text">
                                    <span className="settings-row__title">Personal data</span>
                                    <span className="settings-row__sub">View or update your details</span>
                                </span>
                            </button>
                            <PrivacyToggle />
                        </div>
                    </>
                )}
            </div>
        </aside>
    );
}
