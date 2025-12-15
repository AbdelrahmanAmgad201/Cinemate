import { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import EditPost from '../../components/EditPost';
import { updatePostApi, deletePostApi } from '../../api/post-api';
import PostMain from '../../components/PostMain';
import PostComments from '../../components/PostComments';
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

    const handleDelete = async () => {
        if (!window.confirm('Are you sure you want to delete this post?')) {
            return;
        }

        try {
            const result = await deletePostApi({ postId: post.postId });
            if (result.success) {
                navigate(`/forum/${post.forumId}`);
            } else {
                console.log(result.message || 'Failed to delete post');
            }
        } catch (error) {
            console.error('Error delete post:', error);
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
                postOwnerId={post?.ownerId}
                onCommentCountChange={handleCommentCountChange}
            />
        </div>
    );
};

export default PostFullPage;