import React, { useState, useRef, useEffect, useContext } from 'react';
import { IoIosPerson } from 'react-icons/io';
import { Link } from 'react-router-dom';
import { PATHS } from '../constants/constants.jsx';
import './style/UserProfileSidebar.css';
import { getIsPublicApi, setIsPublicApi } from '../api/user-api.jsx';
import { ToastContext } from '../context/ToastContext.jsx';

function AboutBlock({ user, profile }) {
    const [aboutExpanded, setAboutExpanded] = useState(false);
    const [aboutOverflow, setAboutOverflow] = useState(false);
    const aboutRef = useRef(null);
    const aboutText = (profile && profile.aboutMe && profile.aboutMe.trim()) ? profile.aboutMe : (user && user.about && user.about.trim()) ? user.about : 'No info about the user';

    useEffect(() => {
        const el = aboutRef.current;
        if (!el) return;
        const check = () => {
            setAboutOverflow(el.scrollHeight > el.clientHeight + 1);
        };
        check();
        window.addEventListener('resize', check);
        return () => { window.removeEventListener('resize', check); };
    }, [aboutText]);

    const toggleAbout = () => { setAboutExpanded(v => !v); };

    const id = `about-${user?.id || 'profile'}`;

    return (
        <div>
            <div id={id} ref={aboutRef} className={`about-text ${aboutExpanded ? 'expanded' : 'collapsed'}`} title={aboutText}>
                {aboutText}
            </div>

            {(aboutText !== 'No info about the user') && (aboutOverflow || aboutExpanded) && (
                <div className="about-toggle-wrap">
                    <button
                        type="button"
                        className="about-toggle below"
                        onClick={toggleAbout}
                        aria-expanded={aboutExpanded}
                        aria-controls={id}
                        aria-label={aboutExpanded ? 'Show less of about' : 'Show more of about'}
                    >
                        {aboutExpanded ? 'show less' : 'show more'}
                    </button>
                </div>
            )}
        </div>
    );
}

import { formatCount } from '../utils/formate.jsx';


function PrivacyToggle({ avatarSrc, clickableInPersonalData = false }) {
    const { showToast } = useContext(ToastContext);
    const [isPublic, setIsPublic] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        let mounted = true;
        const load = async () => {
            setLoading(true);
            try {
                const res = await getIsPublicApi();
                if (!mounted) return;
                if (res.success) setIsPublic(Boolean(res.data));
                else showToast('Failed to load visibility', res.message || 'Unknown error', 'error');
            } catch (err) {
                if (!mounted) return;
                showToast('Failed to load visibility', err?.message || 'Unknown error', 'error');
            } finally {
                if (mounted) setLoading(false);
            }
        };
        load();
        return () => { mounted = false; };
    }, [showToast]);

    const performToggle = async (next) => {
        const prev = isPublic;
        setIsPublic(next);
        setSaving(true);
        try {
            const res = await setIsPublicApi({ isPublic: next });
            if (!res.success) {
                setIsPublic(prev);
                showToast('Failed to update visibility', res.message || 'Unknown error', 'error');
                return false;
            }

            showToast('Saved', next ? 'Profile is public' : 'Profile is private', 'success');
            return true;
        } catch (err) {
            setIsPublic(prev);
            showToast('Failed to update visibility', err?.message || 'Unknown error', 'error');
            return false;
        } finally {
            setSaving(false);
        }
    };

    const handleChange = async (e) => {
        const next = !!e.target.checked;
        await performToggle(next);
    };

    const handleClick = async () => {
        if (loading || saving) return;
        await performToggle(!isPublic);
    };

    const handleKey = async (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            await handleClick();
        }
    };

    if (clickableInPersonalData) {
        return (
            <div
                className="mod-user mod-user-clickable profile-visibility-row"
                role="button"
                tabIndex={0}
                onClick={handleClick}
                onKeyDown={handleKey}
                aria-disabled={loading || saving}
                style={{display:'flex',alignItems:'center',gap:12,marginTop:8}}
            >
                <div className="profile-avatar-circle profile-avatar-small" aria-hidden>
                    {avatarSrc ? <img src={avatarSrc} alt="avatar" /> : <IoIosPerson size={18} />}
                </div>

                <div className="mod-text-wrap" style={{flex:1}}>
                    <div className="mod-text">Profile visibility</div>
                    <div className="mod-sub">
                        {isPublic ? 'Anyone can view your profile' : 'Only you can view your profile'}
                    </div>
                </div>

                <div style={{display:'flex',alignItems:'center',gap:8}}>
                    <input
                        type="checkbox"
                        className="visibility-checkbox"
                        checked={!!isPublic}
                        disabled={loading || saving}
                        readOnly
                        aria-label="Toggle profile visibility"
                    />
                </div>
            </div>
        );
    }

    return (
        <div className="mod-user mod-user-clickable" style={{display:'flex',alignItems:'center',gap:12}}>
            <div className="profile-avatar-circle profile-avatar-small" aria-hidden>
                {avatarSrc ? <img src={avatarSrc} alt="avatar" /> : <IoIosPerson size={18} />}
            </div>

            <div className="mod-text-wrap" style={{flex:1}}>
                <div className="mod-text">Profile visibility</div>
                <div className="mod-sub">
                    {loading ? 'loading...' : (isPublic ? 'Anyone can view your profile' : 'Only you can view your profile')}
                </div>
            </div>

            <div style={{display:'flex',alignItems:'center',gap:8}}>
                <label style={{display:'flex',alignItems:'center',gap:8}}>
                    <input
                        type="checkbox"
                        className="visibility-checkbox"
                        checked={!!isPublic}
                        disabled={loading || saving}
                        onChange={handleChange}
                        aria-label="Toggle profile visibility"
                    />
                </label>
            </div>
        </div>
    );
}

export default function UserProfileSidebar({
    sidebarRef,
    showProfileSidebar,
    displayName,
    user,
    profile,
    isOwnProfile,
    setActive,
    avatarSrc,
    formatAccountAge,
    followersCount = 0,
    followingCount = 0
}) {
    const followers = followersCount ?? 0;
    const following = followingCount ?? 0;
    const userId = user?.id || profile?.id;

    const followersLink = PATHS.USER.FOLLOWERS(userId);
    const followingLink = PATHS.USER.FOLLOWING(userId);

    return (
        <aside ref={sidebarRef} className={`sidebar-col profile-sidebar ${!showProfileSidebar ? 'hidden-by-overlap' : ''}`} aria-hidden={!showProfileSidebar}>
            <div className="sidebar-card">
                <div className="sidebar-top-hero" />
                <h2 className="sidebar-title">{displayName}</h2>

                <div className="sidebar-stats-inline">
                    {isOwnProfile ? (
                        <Link
                            to={followersLink}
                            className="stat-inline stat-inline-clickable"
                            aria-label={`${followers} followers`}
                        >
                            <span className="stat-num-inline" title={String(followers)}>{formatCount(followers)}</span>
                            <span className="stat-label-inline">followers</span>
                        </Link>
                    ) : (
                        <div className="stat-inline" aria-label={`${followers} followers`}>
                            <span className="stat-num-inline" title={String(followers)}>{formatCount(followers)}</span>
                            <span className="stat-label-inline">followers</span>
                        </div>
                    )}
                    {isOwnProfile ? (
                        <Link
                            to={followingLink}
                            className="stat-inline stat-inline-clickable"
                            aria-label={`${following} following`}
                        >
                            <span className="stat-num-inline" title={String(following)}>{formatCount(following)}</span>
                            <span className="stat-label-inline">following</span>
                        </Link>
                    ) : (
                        <div className="stat-inline" aria-label={`${following} following`}>
                            <span className="stat-num-inline" title={String(following)}>{formatCount(following)}</span>
                            <span className="stat-label-inline">following</span>
                        </div>
                    )}
                </div>

                <p className="sidebar-desc">{user && (user.username || '')}</p>

                <div className="sidebar-about">
                    <div className="about-title">About</div>
                    {/* About text with collapse/expand */}
                    <AboutBlock user={user} profile={profile} />
                </div>

                <div className="sidebar-account-age">
                    <div className="stat-box">
                        <span className="stat-num">{formatAccountAge(profile?.createdAt || user?.createdAt)}</span>
                        <span className="stat-label">account age</span>
                    </div>
                </div>

                <hr className="sidebar-divider"/>

                {isOwnProfile && (
                    <div className="sidebar-mods">
                        <h3>Settings</h3>

                        <div className="mod-user mod-user-group" role="group" aria-label="Personal data">
                            <div
                                className="mod-user mod-user-clickable"
                                role="button"
                                tabIndex={0}
                                title="Personal data"
                                onClick={() => setActive && setActive('personal')}
                                onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setActive && setActive('personal'); } }}
                                aria-label="Open personal data"
                            >
                                <div className="profile-avatar-circle profile-avatar-small" aria-hidden>
                                    {avatarSrc ? <img src={avatarSrc} alt="avatar" /> : <IoIosPerson size={18} />}
                                </div>
                                <div className="mod-text-wrap">
                                    <div className="mod-text">personal data</div>
                                    <div className="mod-sub">View/Update personal data</div>
                                </div>
                            </div>

                            <PrivacyToggle avatarSrc={avatarSrc} clickableInPersonalData />
                        </div>
                    </div>
                )}
            </div>
        </aside>
    );
}
