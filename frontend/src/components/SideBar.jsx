import { useContext, useState } from 'react';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import { Home, TrendingUp, Compass, Plus } from 'lucide-react';
import './style/SideBar.css';
import FollowedForums from './FollowedForums.jsx';
import { MAX_LENGTHS, PATHS } from '../constants/constants.jsx';
import { createForumApi } from '../api/forum-api.js';
import { ToastContext } from '../context/ToastContext.jsx';
import Modal from './ui/Modal.jsx';
import Textarea from './ui/Textarea.jsx';
import Button from './ui/Button.jsx';

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
        } catch {
            window.scrollTo(0, 0);
        }
    };

    const [forumName, setForumName] = useState('');
    const [forumDescription, setForumDescription] = useState('');
    const [showForm, setShowForm] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    const { showToast } = useContext(ToastContext);

    const handleAddForum = async (e) => {
        e.preventDefault();
        setSubmitting(true);

        const res = await createForumApi({ name: forumName, description: forumDescription });

        if (res.success) {
            showToast('Success', 'Your forum has been submitted.', 'success');
            setForumName('');
            setForumDescription('');
            setShowForm(false);
        } else {
            showToast('Failed to submit forum', res.message || 'unknown error', 'error');
        }

        setSubmitting(false);
    };

    return (
        <>
            <Modal
                open={showForm}
                onClose={() => setShowForm(false)}
                title="Create a forum"
                footer={
                    <>
                        <Button variant="ghost" onClick={() => setShowForm(false)} disabled={submitting}>Cancel</Button>
                        <Button onClick={handleAddForum} loading={submitting} disabled={!forumName.trim()}>Create forum</Button>
                    </>
                }
            >
                <form className="create-forum-form" onSubmit={handleAddForum}>
                    <Textarea
                        label="Name"
                        rows={2}
                        value={forumName}
                        onChange={(e) => setForumName(e.target.value.slice(0, MAX_LENGTHS.TEXTAREA))}
                        placeholder="e.g. Sci-Fi Enthusiasts"
                        maxLength={MAX_LENGTHS.TEXTAREA}
                        required
                    />
                    <Textarea
                        label="Description"
                        rows={4}
                        value={forumDescription}
                        onChange={(e) => setForumDescription(e.target.value.slice(0, MAX_LENGTHS.TEXTAREA))}
                        placeholder="What's this forum about?"
                        maxLength={MAX_LENGTHS.TEXTAREA}
                    />
                </form>
            </Modal>

            <aside className={`user-left-sidebar ${collapsed ? 'collapsed' : ''}`}>
                <nav aria-label="Feed navigation">
                    <ul>
                        <li>
                            <Link to={PATHS.HOME} className={`sidebar-button ${isHomeRoute ? 'active' : ''}`} onClick={handleNavClick}>
                                <Home size={17} aria-hidden="true" /> Home
                            </Link>
                        </li>
                        <li>
                            <Link to={`${PATHS.HOME}?feed=popular`} className={`sidebar-button ${isPopular ? 'active' : ''}`} onClick={handleNavClick}>
                                <TrendingUp size={17} aria-hidden="true" /> Popular
                            </Link>
                        </li>
                        <li>
                            <Link to={PATHS.FORUM.EXPLORE} className={`sidebar-button ${isExplore ? 'active' : ''}`} onClick={handleNavClick}>
                                <Compass size={17} aria-hidden="true" /> Explore forums
                            </Link>
                        </li>
                    </ul>
                </nav>

                <div className="sidebar-divider" />

                <FollowedForums />

                <button type="button" className="sidebar-create-btn" onClick={() => setShowForm(true)}>
                    <Plus size={16} aria-hidden="true" /> Create a forum
                </button>
            </aside>
        </>
    );
};

export default SideBar;
