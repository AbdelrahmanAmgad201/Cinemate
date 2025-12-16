import { useState, useEffect, useRef, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import EditPost from '../../components/EditPost';
import { updatePostApi, isVotedPostApi } from '../../api/post-api';
import "../../components/style/postCard.css";
import "./style/postFullPage.css";
import { MdKeyboardArrowDown } from "react-icons/md";
import { IoClose } from "react-icons/io5";
import PostCard from '../../components/PostCard';

const PostFullPage = () => {
    const { postId } = useParams();
    const location = useLocation();
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const menuRef = useRef(null);

    const [post, setPost] = useState(location.state?.post || null);
    const [openImage, setOpenImage] = useState(false);
    const [userVote, setUserVote] = useState(0);
    const [voteCount, setVoteCount] = useState(0);
    const [editMode, setEditMode] = useState(false);
    const [sort, setSort] = useState("best");
    const [commentText, setCommentText] = useState("");
    const [postOptions, setPostOptions] = useState(false);

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
                console.error('Update failed:', result.message);
            }
        } catch (error) {
            console.error('Error updating post:', error);
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
                    console.error("Vote check failed:", result.message);
                    setUserVote(0);
                }
            } catch(e) {
                console.error('Error checking vote:', e);
                setUserVote(0);
            }
        }

        checkVote();
    }, [postId, user?.id]);

    useEffect(() => {
        if (post) {
            const totalVotes = (post.upvoteCount || 0) - (post.downvoteCount || 0);
            setVoteCount(totalVotes);
        }
    }, [post]);

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

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setPostOptions(false);
            }
        };

        if (postOptions) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [postOptions]);


    if (!post) {
        return <div>Not Found...</div>;
    }

    if(editMode){
        return (
            <div className="post-page">
                <EditPost post={post} onSave={saveEdit} onCancel={cancelEdit} />
            </div>
        );
    }

    return (
        <div className="post-page">
            <PostCard postBody={post} fullMode={true} />
            {openImage && (
                <div className="view-image-container" onClick={() => setOpenImage(false)}>
                    <div className="view-image">
                        <IoClose className="close-button" onClick={() => setOpenImage(false)} />
                        <img src={post.media} alt={post.title || "Post content"} onClick={(e) => e.stopPropagation()} />
                    </div>
                </div>
            )}
            <div className="comment-input">
            <textarea value={commentText} onChange={(e) => setCommentText(e.target.value)} placeholder="Share your thoughts"/>
            </div>
            <div className="comments-section">
                {/* <h3>Comments</h3> */}
                <div className="comments-sort">
                    <label htmlFor="sort-by">Sort by:</label>
                    <div className="select-wrapper">
                        <select id="sort-by" name="sort-by" value={sort} onChange={(e) => setSort(e.target.value)}>
                            <option value="best">Best</option>
                            <option value="new">New</option>
                        </select>
                        <MdKeyboardArrowDown className="select-arrow" />
                    </div>
                </div>
                <p>No comments yet.</p>
            </div>
        </div>
    );
};

export default PostFullPage;