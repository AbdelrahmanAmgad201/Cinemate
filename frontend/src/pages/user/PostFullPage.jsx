import { useState, useEffect, useRef, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import pic from "../../assets/action.jpg";
import { AuthContext } from '../../context/AuthContext';
import EditPost from '../../components/EditPost';
import { updatePostApi, isVotedPostApi, deletePostApi, votePostApi, updateVotePostApi, deleteVotePostApi } from '../../api/post-api';
import "../../components/style/postCard.css";
import "../../components/style/postFullPage.css";
import { IoIosPerson } from "react-icons/io";
import { BsThreeDots } from "react-icons/bs";
import { BiUpvote, BiDownvote, BiSolidUpvote, BiSolidDownvote } from "react-icons/bi";
import { RiShareForwardLine } from "react-icons/ri";
import { FaRegComment } from "react-icons/fa";
import { MdKeyboardArrowDown } from "react-icons/md";
import { IoClose } from "react-icons/io5";

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
    const ownerIdConverted = post.ownerId ? parseInt(post.ownerId, 10) : null;

    const handleVote = async (voteType) => {
        const previousVote = userVote;
        const newVote = userVote === voteType ? 0 : voteType;
        
        const voteDifference = newVote - previousVote;
        
        setUserVote(newVote);
        setVoteCount(prevCount => prevCount + voteDifference);

        try{
            let result;
            console.log("postId: ", postId, "value: ", newVote);
            if(previousVote === 0 && newVote !== 0){
                console.log("Creating vote - postId:", postId, "value:", newVote);
                result = await votePostApi({ postId: postId, value: newVote });
                if (result.success) {
                    console.log("Vote created");
                }
            }
            else if (newVote === 0 && previousVote !== 0){
                console.log("Deleting vote - voteId:", postId);
                result = await deleteVotePostApi({ targetId: postId });
                
                
                if (result.success) {
                    console.log("Vote deleted");
                }
            }

            else if (previousVote !== 0 && newVote !== 0) {
                console.log("Updating vote - voteId:", "value:", newVote);
                result = await updateVotePostApi({ postId: postId, value: newVote });
            }
            if (!result?.success) {
                setUserVote(previousVote);
                setVoteCount(prevCount => prevCount - voteDifference);
                console.error('Vote failed:', result?.message);
            }
        }
        catch(e){
            setUserVote(previousVote);
            setVoteCount(prevCount => prevCount - voteDifference);
            console.error('Vote error:', e);
        }
    };

    const handleDelete = async () => {
        if (!window.confirm('Are you sure you want to delete this post?')) {
            return;
        }

        setPostOptions(false);

        try{
            const result = await deletePostApi({
                postId: post.postId
            });

            if(result.success){
                console.log('Post deleted successfully');
                navigate(`/forum/${post.forumId}`);
            }

            else{
                console.log(result.message || 'Failed to delete post');
            }
        }
        catch(error){
            console.error('Error delete post:', error);
        } 

    }

    const handleEdit = () => {
        setEditMode(true);
        setPostOptions(false);
    };

    const cancelEdit = () => {
        setEditMode(false);
    };

    const saveEdit = async (updatedPost, mediaFile) => {
        try {
            const result = await updatePostApi({
                postId: post.id, 
                forumId: post.forumId,
                title: updatedPostData.title,
                content: updatedPostData.content
            });

            if (result.success) {
                const updatedPost = {
                    ...post,
                    title: updatedPostData.title,
                    content: updatedPostData.content,
                    media: updatedPostData.media
                };
                onSave(updatedPost, mediaFile);
            } else {
                console.error('Update failed:', result.message);
            }
        } catch (error) {
            console.error('Error updating post:', error);
        }
        setPost(updatedPost);
        setEditMode(false);
        console.log('Post updated:', updatedPost, mediaFile);
    };

    useEffect(() => {
        const checkVote = async () => {
            if (!postId || !user?.id) {
                return;
            }

            try {
                // console.log("Checking vote for postId:", postId);
                const result = await isVotedPostApi({ targetId: postId });
                
                if (result.success) {
                    console.log("Vote check result:", result.data);
                    const voteValue = typeof result.data === 'number' ? result.data : 0;
                    setUserVote(voteValue);
                    console.log("User vote set to:", voteValue);
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
            console.log("upvotecount: ", post.upvoteCount, "downvoteCount: ",post.downvoteCount )
            const totalVotes = (post.upvoteCount || 0) - (post.downvoteCount || 0);
            console.log("Setting initial vote count:", totalVotes);
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




    const viewerMenu = [
        { label: "Follow", onClick: () => console.log("Follow clicked") }
    ];
    
    const authorMenu = [
        { label: "Edit", onClick: handleEdit },
        { label: "Delete", onClick: handleDelete }
    ];

    if (!post) {
        return <div>Loading...</div>;
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
            <article className="post-card">
                <div className="post-header">
                    <div className="user-profile-pic">
                        {post.avatar ? post.avatar : <IoIosPerson />}
                    </div>
                    <div className="user-info">
                        <h2 className="user-name">{user.id}</h2>
                        <time dateTime={post.time}>{post.time}</time>
                    </div>
                    <div className="post-settings" ref={menuRef}>
                        {ownerIdConverted === user.id && ( 
                            <>
                            <BsThreeDots onClick={() => setPostOptions(prev => !prev)}/>
                            {postOptions && (
                                <div className="options-menu">
                                <ul>
                                {(ownerIdConverted === user?.id ? authorMenu : viewerMenu).map((item, index) => (
                                    <li key={index} onClick={item.onClick}>{item.label}</li>
                                ))}
                                </ul>
                            </div>
                            )}
                            </>
                        )}
                        
                    </div>
                </div>
                <div className="post-content">
                    <div className="post-title" >
                        <p>{post.title}</p>
                    </div>
                    <div className="post-media" >
                        {post.media && <img src={post.media} alt={post.title || "Post content"} onClick={() => setOpenImage(true)}/>}
                        {post.content && <p className="post-text">{post.content}</p>}
                    </div>
                </div>
                <footer className="post-footer">
                    <div className="up-down-vote">
                        {userVote === 1 ? (
                            <BiSolidUpvote className="selected" onClick={() => handleVote(1)} />
                        ) : (
                            <BiUpvote onClick={() => handleVote(1)} />
                        )}
                        <span className="vote-count">{voteCount}</span>
                        <span className="vote-separator">|</span>
                        {userVote === -1 ? (
                            <BiSolidDownvote className="selected" onClick={() => handleVote(-1)} />
                        ) : (
                            <BiDownvote onClick={() => handleVote(-1)} />
                        )}
                    </div>
                    <div className="post-comment">
                        <FaRegComment />
                    </div>
                    {/* <div className="post-share">
                        <RiShareForwardLine />
                    </div> */}
                </footer>
            </article>
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