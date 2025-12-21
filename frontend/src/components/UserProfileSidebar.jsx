import React, { useState, useRef, useEffect } from 'react';
import { IoIosPerson } from 'react-icons/io';
import './style/UserProfileSidebar.css';

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

export default function UserProfileSidebar({
    sidebarRef,
    showProfileSidebar,
    displayName,
    user,
    profile,
    isOwnProfile,
    setActive,
    avatarSrc,
    formatAccountAge
}) {
    return (
        <aside ref={sidebarRef} className={`sidebar-col profile-sidebar ${!showProfileSidebar ? 'hidden-by-overlap' : ''}`} aria-hidden={!showProfileSidebar}>
            <div className="sidebar-card">
                <div className="sidebar-top-hero" />
                <h2 className="sidebar-title">{displayName}</h2>
                <p className="sidebar-desc">{user && (user.username || '')}</p>

                <div className="sidebar-about">
                    <div className="about-title">About</div>
                    {/* About text with collapse/expand */}
                    <AboutBlock user={user} profile={profile} />
                </div>

                <div className="sidebar-stats row">
                    <div className="stat-box">
                        <span className="stat-num">{formatAccountAge(profile?.createdAt || user?.createdAt)}</span>
                        <span className="stat-label">account age</span>
                    </div>
                </div>

                <hr className="sidebar-divider"/>

                {isOwnProfile && (
                    <div className="sidebar-mods">
                        <h3>Settings</h3>
                        <div
                            className="mod-user mod-user-clickable"
                            role="button"
                            tabIndex={0}
                            onClick={() => setActive('personal')}
                            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setActive('personal'); } }}
                        >
                            <div className="profile-avatar-circle profile-avatar-small" aria-hidden>
                                {avatarSrc ? <img src={avatarSrc} alt="avatar" /> : <IoIosPerson size={18} />}
                            </div>
                            <div className="mod-text-wrap">
                                <div className="mod-text">personal data</div>
                                <div className="mod-sub">View/Update personal data</div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </aside>
    );
}
