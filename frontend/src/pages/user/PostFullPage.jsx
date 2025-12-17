import { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import { ToastContext } from '../../context/ToastContext';
import Swal from 'sweetalert2';
import EditPost from '../../components/EditPost';
import { updatePostApi, deletePostApi } from '../../api/post-api';
import PostMain from '../../components/PostMain';
import PostComments from '../../components/PostComments';
import { PATHS } from '../../constants/constants';
import '../../components/style/postCard.css';
import '../../components/style/postFullPage.css';

const PostFullPage = () => {
    const { postId } = useParams();
    const location = useLocation();
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();

    const [post, setPost] = useState(location.state?.post || null);
    const [editMode, setEditMode] = useState(false);

    useEffect(() => {
        if (location.state?.post) {
            return;
        }
    }, [postId, location.state]);

    useEffect(() => {
        if (location.state?.editMode) {
            setEditMode(true);
            navigate(location.pathname, { replace: true, state: {} });
        }
    }, [postId, location.state, location.pathname, navigate]);

    const [loadingPost, setLoadingPost] = useState(false);
    const [postLoadError, setPostLoadError] = useState(null);

    useEffect(() => {
        const suppliedPost = location.state?.post;
        const pid = suppliedPost?.id || suppliedPost?.postId || location.state?.postId || postId;
        if (suppliedPost) {
            setPost(suppliedPost);
            setPostLoadError(null);
            return;
        }
        if (!pid) return;
        try {
            const cached = sessionStorage.getItem(`CINEMATE_LAST_POST_${pid}`);
            if (cached) {
                const parsed = JSON.parse(cached);
                setPost(parsed);
                setPostLoadError(null);
                return;
            }
        } catch (e) {
            // ignore storage errors
        }
        setPost({ id: pid, postId: pid, title: 'Post', content: '', media: null, ownerId: null, commentCount: 0, isPlaceholder: true });
        setPostLoadError(null);
    }, [postId, location.state]);

    useEffect(() => {
        const commentIdToScroll = location.state?.commentId;
        if (!commentIdToScroll) return;
        let attempts = 0;
        let cancelled = false;
        const timers = [];
        const tryScroll = () => {
            if (cancelled) return;
            const el = document.getElementById(`comment-${commentIdToScroll}`);
            if (el) {
                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                el.classList.add('highlight');
                const t = setTimeout(() => el.classList.remove('highlight'), 3000);
                timers.push(t);
                navigate(location.pathname, { replace: true, state: {} });
                return;
            }
            attempts++;
            if (attempts < 20) {
                const t = setTimeout(tryScroll, 150);
                timers.push(t);
            } else {
                navigate(location.pathname, { replace: true, state: {} });
            }
        };
        tryScroll();
        return () => {
            cancelled = true;
            timers.forEach(t => clearTimeout(t));
        };
    }, [post, location.state, location.pathname, navigate]);

    const { showToast } = useContext(ToastContext);

    const handleDelete = async () => {
        const result = await Swal.fire({
            title: 'Delete post?',
            text: 'Are you sure you want to delete this post?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, delete',
            confirmButtonColor: '#d33',
            cancelButtonText: 'Cancel',
        });

        if (!result.isConfirmed) return;

        try {
            const pid = post?.id || post?.postId || postId;
            if (!pid) {
                console.error('Delete aborted: no postId available', { post });
                showToast('Error', 'Could not determine post id to delete', 'error');
                return;
            }
            const res = await deletePostApi({ postId: pid });
            if (res.success) {
                showToast('Deleted', 'Post removed', 'info');
                navigate(PATHS.HOME);
            } else {
                console.log(res.message || 'Failed to delete post');
                showToast('Error', res.message || 'Failed to delete post', 'error');
            }
        } catch (error) {
            console.error('Error delete post:', error);
            showToast('Error', 'Failed to delete post', 'error');
        }
    };

    const handleEdit = () => {
        setEditMode(true);
    };

    const cancelEdit = () => {
        setEditMode(false);
    };

    const saveEdit = async (updatedPost) => {
        try {
            const result = await updatePostApi({
                postId: post.id,
                forumId: post.forumId,
                title: updatedPost.title,
                content: updatedPost.content
            });

            if (result.success) {
                setPost({
                    ...post,
                    title: updatedPost.title,
                    content: updatedPost.content,
                    media: updatedPost.media
                });
                setEditMode(false);
            } else {
                console.error('Update failed:', result.message);
            }
        } catch (error) {
            console.error('Error updating post:', error);
        }
    };

    const handleCommentCountChange = (count) => {
        setPost(prev => (prev ? { ...prev, commentCount: count } : prev));
    };

    if (loadingPost) {
        return <div>Loading...</div>;
    }

    if (postLoadError) {
        return (
            <div className="post-page" style={{ padding: 24 }}>
                <h2>Error</h2>
                <p>Failed to load post: {postLoadError}</p>
            </div>
        );
    }

    if (!post) {
        return <div>Loading...</div>;
    }

    if (editMode) {
        return (
            <div className="post-page">
                <EditPost post={post} onSave={saveEdit} onCancel={cancelEdit} />
            </div>
        );
    }

    return (
        <div className="post-page">
            <PostMain
                post={post}
                user={user}
                onEdit={handleEdit}
                onDelete={handleDelete}
            />
            <PostComments
                postId={post?.id || post?.postId || postId}
                post={post}
                postOwnerId={post?.ownerId}
                onCommentCountChange={handleCommentCountChange}
            />
        </div>
    );
};

export default PostFullPage;