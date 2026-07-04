import './style/Forum.css';
import { Calendar, Plus } from 'lucide-react';
import { useCallback, useContext, useEffect, useState } from 'react';
import { PATHS, MAX_LENGTHS, PAGE_SIZE } from '../../constants/constants.jsx';
import { formatCount } from '../../utils/formate.jsx';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { checkFollowApi, followForumApi, unfollowForumApi, getForumPostsApi, getForumApi, getModApi } from '../../api/forum-api.js';
import PostsFeed from '../../components/PostsFeed.jsx';
import { addPostApi } from '../../api/post-api.js';

import { AuthContext } from '../../context/AuthContext.jsx';
import { ToastContext } from '../../context/ToastContext.jsx';
import SortSelector from '../../components/SortSelector.jsx';
import Avatar from '../../components/ui/Avatar.jsx';
import Button from '../../components/ui/Button.jsx';
import Modal from '../../components/ui/Modal.jsx';
import Textarea from '../../components/ui/Textarea.jsx';
import EmptyState from '../../components/ui/EmptyState.jsx';

import { SORT_OPTIONS } from '../../constants/uiConstants.jsx';

export default function Forum() {
    const { showToast } = useContext(ToastContext);
    const { user } = useContext(AuthContext);
    const { forumId } = useParams();
    const navigate = useNavigate();

    const [posts, setPosts] = useState([]);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState();

    const [forumName, setForumName] = useState();
    const [forumDescription, setForumDescription] = useState();
    const [forumCreationDate, setForumCreationDate] = useState();
    const [followersCount, setFollowersCount] = useState();
    const [postsCount, setPostsCount] = useState();

    const [moderators, setModerators] = useState([]);
    const isMod = !!(user && moderators.some((m) => m.id === user.id));
    const [isJoined, setIsJoined] = useState(false);

    useEffect(() => {
        const fetchForumDetails = async () => {
            const res = await getForumApi({ forumId });

            if (!res.success) {
                showToast('Failed to fetch forum details', res.message || 'unknown error', 'error');
                setError(res.message);
                navigate(PATHS.HOME);
                return;
            }

            const data = res.data;
            setError(null);
            setForumName(data.name);
            setForumDescription(data.description);
            setForumCreationDate(new Date(data.createdAt).toDateString());
            setFollowersCount(data.followerCount);
            setPostsCount(data.postCount);
            const res2 = await getModApi({ userId: data.ownerId24Bit });
            setModerators([{ id: data.ownerId, username: res2.data, avatar: null }]);
        };

        const checkStatus = async () => {
            try {
                const res = await checkFollowApi({ forumId });
                if (res && res.data) setIsJoined(true);
            } catch (error) {
                console.error('Failed to check join status', error);
            }
        };

        if (forumId) {
            fetchForumDetails();
            checkStatus();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [forumId]);

    const handleJoin = async () => {
        if (isJoined) {
            const res = await unfollowForumApi({ forumId });
            if (res.success) {
                showToast('Success', 'You have left the forum.', 'success');
                setIsJoined(false);
                return;
            }
            showToast('Failed to leave forum', res.message || 'unknown error', 'error');
            return;
        }

        const res = await followForumApi({ forumId });
        if (res.success) {
            showToast('Success', 'You have joined the forum.', 'success');
            setIsJoined(true);
            return;
        }
        showToast('Failed to join forum', res.message || 'unknown error', 'error');
    };

    const [activeSort, setActiveSort] = useState(SORT_OPTIONS[0]);

    const [showForm, setShowForm] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [postTitle, setPostTitle] = useState('');
    const [postText, setPostText] = useState('');

    const fetchPosts = useCallback(async (pageNum, sort) => {
        setLoading(true);

        const res = await getForumPostsApi({ forumId, page: pageNum, size: PAGE_SIZE.FORUM, sort });

        if (!res.success) {
            setHasMore(false);
            setLoading(false);
            return;
        }

        const newPosts = res.data.posts;
        setPosts((prevPosts) => (pageNum === 0 ? newPosts : [...prevPosts, ...newPosts]));
        if (newPosts.length < PAGE_SIZE.FORUM) setHasMore(false);

        setLoading(false);
    }, [forumId]);

    const handleAddPost = async (e) => {
        e.preventDefault();
        setSubmitting(true);

        const res = await addPostApi({ forumId, title: postTitle, content: postText });

        if (res.success) {
            showToast('Success', 'Your post has been submitted.', 'success');
            setPostTitle('');
            setPostText('');
            setShowForm(false);

            setPage(0);
            setHasMore(true);
            setPosts([]);
            await fetchPosts(0, activeSort.value);
        } else {
            showToast('Failed to submit post', res.message || 'unknown error', 'error');
        }

        setSubmitting(false);
    };

    useEffect(() => {
        setPosts([]);
        setPage(0);
        setHasMore(true);
        fetchPosts(0, activeSort.value);
    }, [forumId, activeSort, fetchPosts]);

    const handleLoadMore = () => {
        const nextPage = page + 1;
        setPage(nextPage);
        fetchPosts(nextPage, activeSort.value);
    };

    if (error != null) {
        return (
            <EmptyState
                title="Forum not found"
                description="This forum may have been removed or the link is incorrect."
                actionLabel="Back to home"
                onAction={() => navigate(PATHS.HOME)}
            />
        );
    }

    return (
        <div className="forum-container">
            <Modal
                open={showForm}
                onClose={() => setShowForm(false)}
                title={`Create post in ${forumName || ''}`}
                footer={
                    <>
                        <Button variant="ghost" onClick={() => setShowForm(false)} disabled={submitting}>Cancel</Button>
                        <Button onClick={handleAddPost} loading={submitting} disabled={!postTitle.trim()}>Add post</Button>
                    </>
                }
            >
                <form className="create-post-form" onSubmit={handleAddPost}>
                    <Textarea
                        label="Title"
                        rows={2}
                        value={postTitle}
                        onChange={(e) => setPostTitle(e.target.value.slice(0, MAX_LENGTHS.TEXTAREA))}
                        placeholder={`Title (max ${MAX_LENGTHS.TEXTAREA} characters)`}
                        maxLength={MAX_LENGTHS.TEXTAREA}
                        required
                    />
                    <Textarea
                        label="Body"
                        rows={5}
                        value={postText}
                        onChange={(e) => setPostText(e.target.value.slice(0, MAX_LENGTHS.TEXTAREA))}
                        placeholder={`Body text (max ${MAX_LENGTHS.TEXTAREA} characters)`}
                        maxLength={MAX_LENGTHS.TEXTAREA}
                    />
                </form>
            </Modal>

            <div className="forum-banner" />
            <div className="forum-header">
                <div className="header-left">
                    <Avatar name={forumName} size="xl" />
                    <h1>{forumName}</h1>
                </div>

                <div className="header-right">
                    <Button variant="secondary" icon={<Plus size={16} />} onClick={() => setShowForm(true)}>Create post</Button>
                    {!isMod
                        ? <Button variant={isJoined ? 'secondary' : 'primary'} onClick={handleJoin}>{isJoined ? 'Leave' : 'Join'}</Button>
                        : <Button onClick={() => navigate(PATHS.MOD.PAGE(forumId))}>Mod tools</Button>}
                </div>
            </div>

            <div className="forum-main-grid">
                <main className="feed-col">
                    <SortSelector currentSort={activeSort} options={SORT_OPTIONS} onSortChange={setActiveSort} />

                    <PostsFeed
                        posts={posts}
                        loading={loading}
                        hasMore={hasMore}
                        onLoadMore={handleLoadMore}
                        emptyMessage="No posts here yet."
                    />
                </main>

                <aside className="sidebar-col">
                    <div className="sidebar-card">
                        <h2 className="sidebar-title">{forumName}</h2>
                        <p className="sidebar-desc">{forumDescription}</p>

                        <span className="sidebar-meta">
                            <Calendar size={15} />
                            Created {forumCreationDate}
                        </span>

                        <hr className="sidebar-divider" />

                        <div className="sidebar-stats row">
                            <div className="stat-box">
                                <span className="stat-num">{formatCount(followersCount)}</span>
                                <span className="stat-label">Followers</span>
                            </div>
                            <div className="stat-box">
                                <span className="stat-num">{formatCount(postsCount)}</span>
                                <span className="stat-label">Posts</span>
                            </div>
                        </div>

                        <hr className="sidebar-divider" />

                        <div className="sidebar-mods">
                            <h3>Moderators</h3>
                            {moderators.map(({ id, username }) => (
                                <div className="mod-user" key={id}>
                                    <Link to={PATHS.USER.PROFILE(id)}>
                                        <Avatar name={username} size="xs" />
                                    </Link>
                                    <span>
                                        <Link to={PATHS.USER.PROFILE(id)}>{username}</Link>
                                        {user && user.id === id && <span className="mod-badge">you</span>}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    );
}
