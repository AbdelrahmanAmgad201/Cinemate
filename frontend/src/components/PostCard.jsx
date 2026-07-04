import { useState, useEffect, useRef, useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MoreVertical, MessageCircle } from 'lucide-react';
import './style/postCard.css';
import { formatDistanceToNow } from 'date-fns';
import { deletePostApi, getForumNameApi } from '../api/post-api.js';
import { getModApi } from '../api/forum-api.js';
import VoteWidget from './VoteWidget';
import { AuthContext } from '../context/AuthContext.jsx';
import { ToastContext } from '../context/ToastContext.jsx';
import { PATHS } from '../constants/constants';
import Avatar from './ui/Avatar.jsx';
import ConfirmDialog from './ui/ConfirmDialog.jsx';

const PostCard = ({ postBody, fullMode = false, showForumName = false }) => {
    const [postOptions, setPostOptions] = useState(false);
    const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
    const [firstName, setFirstName] = useState('');
    const [forumName, setForumName] = useState('');
    const [loading, setLoading] = useState(true);
    const [commentCount, setCommentCount] = useState(null);
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);
    const navigate = useNavigate();
    const menuRef = useRef(null);

    // Listen for comment count update events
    useEffect(() => {
        function handleCommentCountUpdate(e) {
            if (e.detail && (e.detail.postId === postBody.id || e.detail.postId === postBody.postId)) {
                const n = Number(e.detail.commentCount);
                const sanitized = Number.isFinite(n) ? Math.max(0, Math.trunc(n)) : 0;
                setCommentCount(sanitized);
            }
        }
        window.addEventListener('postCommentCountUpdated', handleCommentCountUpdate);
        return () => window.removeEventListener('postCommentCountUpdated', handleCommentCountUpdate);
    }, [postBody.id, postBody.postId]);

    // Set initial comment count from post data if available
    useEffect(() => {
        if (commentCount === null && postBody.commentCount != null) {
            setCommentCount(postBody.commentCount);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [postBody.commentCount]);

    const formattedTime = postBody.createdAt ? formatDistanceToNow(new Date(postBody.createdAt), { addSuffix: true }) : 'Recently';

    const navigateToPost = () => {
        if (fullMode) return;
        try {
            sessionStorage.setItem(`CINEMATE_LAST_POST_${postBody.id}`, JSON.stringify(postBody));
        } catch {
            // ignore storage errors
        }
        navigate(PATHS.POST.FULLPAGE(postBody.id));
    };

    const handleDelete = async () => {
        setConfirmDeleteOpen(false);
        setPostOptions(false);

        const res = await deletePostApi({ postId: postBody.id });

        if (res.success) {
            showToast('', 'Post deleted', 'success');
            navigate(`/forum/${postBody.forumId}`);
        } else {
            showToast('Failed to delete post', res.message || 'unknown error', 'error');
        }
    };

    const handleEdit = () => {
        if (fullMode) {
            navigate('.', { state: { editMode: true }, replace: true });
            return;
        }
        setPostOptions(false);
        navigate(PATHS.POST.FULLPAGE(postBody.id), { state: { post: postBody, editMode: true } });
    };

    const authorMenu = [
        { label: 'Edit', onClick: handleEdit },
        { label: 'Delete', onClick: () => setConfirmDeleteOpen(true) },
    ];

    useEffect(() => {
        const initializePost = async () => {
            try {
                const requests = [getModApi({ userId: postBody.ownerId })];
                if (showForumName) requests.push(getForumNameApi({ forumId: postBody.forumId }));

                const results = await Promise.all(requests);

                if (results[0]?.data) setFirstName(results[0].data);
                if (showForumName && postBody.forumId && results[1]?.data) setForumName(results[1].data);

                setLoading(false);
            } catch (error) {
                showToast('Failed to fetch post', error?.message || 'unknown error', 'error');
                setLoading(false);
            }
        };

        initializePost();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [postBody]);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setPostOptions(false);
            }
        };

        if (postOptions) document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [postOptions]);

    const ownerIdConverted = postBody.ownerId ? parseInt(postBody.ownerId, 10) : null;
    const isOwner = ownerIdConverted === user.id;

    return (
        <article className="post-card">
            <div className="post-header">
                <button type="button" className="post-header__avatar-btn" onClick={() => navigate(PATHS.USER.PROFILE(ownerIdConverted))}>
                    <Avatar name={loading ? undefined : firstName} size="md" />
                </button>

                <div className="user-info">
                    {showForumName && (
                        <p className="forum-name" onClick={() => navigate(PATHS.FORUM.PAGE(postBody.forumId))}>
                            {loading ? 'Loading…' : forumName}
                        </p>
                    )}
                    <Link className="user-name" to={PATHS.USER.PROFILE(ownerIdConverted)}>{loading ? 'Loading…' : firstName}</Link>
                    <time dateTime={postBody.createdAt} className="post-time">{formattedTime}</time>
                </div>

                {isOwner && (
                    <div className="post-settings" ref={menuRef}>
                        <button type="button" className="post-settings__trigger" onClick={() => setPostOptions((p) => !p)} aria-label="Post options">
                            <MoreVertical size={18} />
                        </button>
                        {postOptions && (
                            <div className="options-menu">
                                <ul>
                                    {authorMenu.map((item, index) => (
                                        <li key={index} onClick={item.onClick}>{item.label}</li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                )}
            </div>

            <div className="post-content" onClick={navigateToPost}>
                <div className="post-title"><p>{postBody.title}</p></div>
                <div className="post-media">
                    {postBody.content && <p className="post-text">{postBody.content}</p>}
                    {postBody.media && <img src={postBody.media} alt={postBody.title || 'Post content'} />}
                </div>
            </div>

            <footer className="post-footer">
                <VoteWidget targetId={postBody.id} initialUp={postBody.upvoteCount} initialDown={postBody.downvoteCount} isPost />
                <button type="button" className="post-comment" onClick={navigateToPost}>
                    <MessageCircle size={16} />
                    <span className="comment-count">{commentCount !== null ? commentCount : (postBody.commentCount || 0)}</span>
                </button>
            </footer>

            <ConfirmDialog
                open={confirmDeleteOpen}
                onClose={() => setConfirmDeleteOpen(false)}
                onConfirm={handleDelete}
                title="Delete post?"
                message="Are you sure you want to delete this post? This can't be undone."
                confirmLabel="Delete"
                danger
            />
        </article>
    );
};

export default PostCard;
