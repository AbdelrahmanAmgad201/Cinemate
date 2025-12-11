import React, {useContext, useState} from 'react';
import { Link, useLocation } from 'react-router-dom';
import "./style/SideBar.css";
import FollowedForums from './FollowedForums.jsx';
import {MAX_LENGTHS, PATHS} from '../constants/constants.jsx';
import { useSearchParams } from 'react-router-dom';
import {createForumApi} from "../api/forum-api.jsx";
import {ToastContext} from "../context/ToastContext.jsx";

const SideBar = ({ collapsed }) => {
    const location = useLocation();
    const [searchParams] = useSearchParams();
    const feed = searchParams.get('feed') || 'following';

    const isHomeRoute = location.pathname === PATHS.HOME && (feed === 'following' || !searchParams.get('feed'));
    const isPopular = location.pathname === PATHS.HOME && feed === 'popular';
    const isExplore = location.pathname === PATHS.FORUM.EXPLORE;

    const handleNavClick = () => {
        try {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } catch (e) {
            window.scrollTo(0, 0);
        }
    };

    const [forumName, setForumName] = useState("");
    const [forumDescription, setForumDescription] = useState("");

    const [showForm, setShowForm] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const {showToast} = useContext(ToastContext);

    const handleAddForum = async (e) => {
        e.preventDefault();

        setSubmitting(true);

        const res = await createForumApi({ name: forumName, description: forumDescription})

        if (res.success === true) {
            showToast("Success", "Your forum has been submitted.", "success")
            setForumName("");
            setForumDescription("");
            setShowForm(false);
        }
        else {
            showToast("Failed to submit forum", res.message || "unknown error", "error")
        }

        setSubmitting(false);
    }

    return (
        <>
            {showForm && (
                <div className="modal-overlay" onMouseDown={() => setShowForm(false)}>
                    <div className="modal" onMouseDown={(e) => e.stopPropagation()}>
                        <h3>Create forum</h3>
                        <div className="header-left" style={{alignItems: "center", marginBottom: "10px"}}>
                            <div className="forum-icon-placeholder" style={{width: "50px", height: "50px"}}></div>
                            <h1 style={{fontSize: "16px"}}>{forumName}</h1>
                        </div>
                        <form onSubmit={handleAddForum}>

                            <label>
                                Title
                                <textarea
                                    rows="3"
                                    value={forumName} onChange={e => {
                                    const inputValue = e.target.value;
                                    if (inputValue.length <= MAX_LENGTHS.TEXTAREA) {
                                        setForumName(inputValue);
                                    }
                                }}
                                    placeholder={`Name (max ${MAX_LENGTHS.TEXTAREA} characters)`}
                                    maxLength={MAX_LENGTHS.TEXTAREA}
                                    required
                                />
                                <small>{forumName.length} / {MAX_LENGTHS.TEXTAREA} characters</small>
                            </label>

                            <label>
                                Body
                                <textarea
                                    rows="5"
                                    value={forumDescription} onChange={e => {
                                    const inputValue = e.target.value;
                                    if (inputValue.length <= MAX_LENGTHS.TEXTAREA) {
                                        setForumDescription(inputValue);
                                    }
                                }}
                                    placeholder={`Description (max ${MAX_LENGTHS.TEXTAREA} characters)`}
                                    maxLength={MAX_LENGTHS.TEXTAREA}
                                />
                                <small>{forumDescription.length} / {MAX_LENGTHS.TEXTAREA} characters</small>
                            </label>
                            <div className="modal-actions">
                                <button type="button" className="modal-btn-cancel" onClick={() => setShowForm(false)}>Cancel</button>
                                <button type="submit" className="modal-btn-submit" disabled={submitting}>{submitting ? "Adding..." : "Add Forum"}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

                <aside className={`user-left-sidebar ${collapsed ? 'collapsed' : ''}`}>
                <ul>
                    <li>
                        <Link to={`${PATHS.HOME}`} className={`sidebar-button ${isHomeRoute ? 'active' : ''}`} onClick={handleNavClick}>
                            Home
                        </Link>
                    </li>
                    <li>
                        <Link to={`${PATHS.HOME}?feed=popular`} className={`sidebar-button ${isPopular ? 'active' : ''}`} onClick={handleNavClick}>
                            Popular
                        </Link>
                    </li>
                    <li>
                        <Link to={`${PATHS.FORUM.EXPLORE}`} className={`sidebar-button ${isExplore ? 'active' : ''}`} onClick={handleNavClick}>
                            Explore forums
                        </Link>
                    </li>
                    <li>
                        <FollowedForums />
                    </li>
                    <li>
                        <div className="sidebar-button" onClick={() => setShowForm(true)}> + Create a forum</div>
                    </li>
                </ul>
            </aside>
        </>
    );
};

export default SideBar;