import React from 'react';
import { IoIosPerson } from 'react-icons/io';
import './style/UserProfileSidebar.css';

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
                    <div className="about-text">{(profile && profile.aboutMe && profile.aboutMe.trim()) ? profile.aboutMe : (user && user.about && user.about.trim()) ? user.about : 'No info about the user'}</div>
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
