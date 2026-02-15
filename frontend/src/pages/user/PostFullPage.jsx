import { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import { ToastContext } from '../../context/ToastContext.jsx';
import Swal from 'sweetalert2';
import EditPost from '../../components/EditPost';
import { updatePostApi, deletePostApi, isVotedPostApi, getPostApi } from '../../api/post-api';
import PostMain from '../../components/PostMain';
import PostComments from '../../components/PostComments';
import { PATHS } from '../../constants/constants';
import "../../components/style/postCard.css";
import "./style/postFullPage.css";
import { IoClose } from "react-icons/io5";
import PostCard from '../../components/PostCard';

const PostFullPage = () => {
    const { postId } = useParams();
    const location = useLocation();
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);
    const navigate = useNavigate();

    const [post, setPost] = useState({
        commentCount: 0,
        content: '',
        createdAt: null,
        deletedAt: null,
        downvoteCount: 0,
        forumId: 0,
        id: postId,
        isDeleted: false,
        lastActivityAt: null,
        ownerId: 0,
        score: 0,
        title: '',
        upvoteCount: 0
    });
    const [editMode, setEditMode] = useState(false);
    const [userVote, setUserVote] = useState(0);
    const [openImage, setOpenImage] = useState(false);


    const cancelEdit = () => {
        setEditMode(false);
    };

    const saveEdit = async (updatedPost, mediaFile) => {
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
                console.log('Post updated successfully');
            } else {
                if (!result.success) return showToast('Failed to edit post', result.message || 'unknown error', 'error');
            }
        } 
        catch(error){
            showToast('Failed to edit post', error || 'unknown error', 'error');
        }
    };

    useEffect(() => {
        const checkVote = async () => {
            if (!postId || !user?.id) {
                return;
            }

            try {
                const result = await isVotedPostApi({ targetId: postId });
                
                if (result.success) {
                    const voteValue = typeof result.data === 'number' ? result.data : 0;
                    setUserVote(voteValue);
                } else {
                    showToast('Failed to check vote', result.message || 'unknown error', 'error');
                    setUserVote(0);
                }
            } 
            catch(error){
                showToast('Failed to check vote', error || 'unknown error', 'error');
                setUserVote(0);
            }
        }

        const fetchPost = async () =>{
            if (!postId || !user?.id) {
                showToast('Failed to fetch post', 'Invalid postId / userId', 'error');
                return;
            }

            try{
                const result = await getPostApi({postId: postId});

                if(result.success){
                    setPost(result.data);
                }
                else{
                    showToast('Failed to fetch post', result.message || 'unknown error', 'error');
                }
            }
            catch(error){
                showToast('Failed to fetch post', error || 'unknown error', 'error');
            }
        }

        checkVote();
        fetchPost();
    }, [postId]);

    useEffect(() => {
        if (location.state?.editMode) {
            setEditMode(true);
            navigate('.', { replace: true, state: {} });
        }
    }, [postId, location.state?.editMode, location.pathname, navigate]);

    const [postLoadError, setPostLoadError] = useState(null);

    useEffect(() => {
        try {
            const cached = sessionStorage.getItem(`CINEMATE_LAST_POST_${postId}`);
            if (cached) {
                const parsed = JSON.parse(cached);
                setPost(parsed);
                setPostLoadError(null);
                return;
            }
        } catch (e) {
            // ignore storage errors
        }
        setPostLoadError(null);
    }, [postId]);

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
    }, [post, location.state?.commentId, location.pathname, navigate]);

    if (!post) {
        return <div>Not Found...</div>;
    }

    if (editMode) {
        return (
            <div className="post-page">
                <EditPost post={post} onSave={saveEdit} onCancel={cancelEdit}/>
            </div>
        );
    }

    return (
        <div className="post-page">
            <div className="post-main-area">
                <PostCard postBody={post} fullMode={true} showForumName={true} />
                {openImage && (
                    <div className="view-image-container" onClick={() => setOpenImage(false)}>
                        <div className="view-image">
                            <IoClose className="close-button" onClick={() => setOpenImage(false)} />
                            <img src={post.media} alt={post.title || "Post content"} onClick={(e) => e.stopPropagation()} />
                        </div>
                    </div>
                )}
                <PostComments
                    postId={post.id}
                    post={post}
                    postOwnerId={post.ownerId}
                    onCommentCountChange={(count) => setPost(prev => prev ? { ...prev, commentCount: count } : prev)}
                />
            </div>
        </div>
    );
};

export default PostFullPage;