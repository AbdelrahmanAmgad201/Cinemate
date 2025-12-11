import { useState, useEffect, useRef, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import pic from "../../assets/action.jpg";
import { AuthContext } from '../../context/AuthContext';
import EditPost from '../../components/EditPost';
import { updatePostApi, deletePostApi, votePostApi, updateVotePostApi, deleteVotePostApi } from '../../api/post-api';
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

    const mockPost = [
        { 
            id: "693a406767a0a5d7e5a91f8f",
            forumId: "693a404167a0a5d7e5a91f8e",
            ownerId: "000000000000000000000001", 
            title: "sdfgh",
            content: "asdfgn",
            votes: 0
        }
        ];

    const [post, setPost] = useState(location.state?.post || null);
    const [openImage, setOpenImage] = useState(false);
    const [userVote, setUserVote] = useState(0);
    const [voteCount, setVoteCount] = useState(0);
    const [voteId, setVoteId] = useState(post?.voteId || null);
    const [editMode, setEditMode] = useState(false);
    const [sort, setSort] = useState("best");
    const [commentText, setCommentText] = useState("");
    const [postOptions, setPostOptions] = useState(false);

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
                result = await votePostApi({
                    targetId: postId,
                    value: newVote
                });
                if (result.success){
                    console.log(result);
                    setVoteId(result.data);
                }
            }
            else if (newVote === 0 && voteId){
                console.log("Deleting vote - voteId:", voteId);
                result = await deleteVote({ voteId });
                
                if (result.success) {
                    console.log(result);
                    setVoteId(null);
                }
            }

            else if (previousVote !== 0 && newVote !== 0 && voteId) {
                console.log("Updating vote - voteId:", voteId, "value:", newVote);
                result = await updateVote({
                    voteId: voteId,
                    value: newVote
                });
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
                navigate('/');
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
                title: editedTitle.trim(),
                content: editedText.trim()
            });

            if (result.success) {
                const updatedPost = {
                    ...post,
                    title: editedTitle.trim(),
                    content: editedText.trim(),
                    text: editedText.trim(),
                    media: addedMedia
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
                        {post.ownerId === user.id && ( 
                            <>
                            <BsThreeDots onClick={() => setPostOptions(prev => !prev)}/>
                            {postOptions && (
                                <div className="options-menu">
                                <ul>
                                {(post.ownerId === user?.id ? authorMenu : viewerMenu).map((item, index) => (
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