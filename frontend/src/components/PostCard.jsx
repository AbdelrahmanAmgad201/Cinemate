import { useState, useEffect, useRef, useContext, use } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { IoIosPerson } from "react-icons/io";
import { BsThreeDots } from "react-icons/bs";
import { BiUpvote, BiDownvote, BiSolidUpvote, BiSolidDownvote } from "react-icons/bi";
import { RiShareForwardLine } from "react-icons/ri";
import { FaRegComment } from "react-icons/fa";
import "./style/postCard.css";
import Swal from "sweetalert2";
import { formatDistanceToNow } from 'date-fns';
import { deletePostApi, isVotedPostApi, deleteVotePostApi, votePostApi, updateVotePostApi, getForumNameApi } from '../api/post-api.jsx';
import { getModApi } from '../api/forum-api.jsx';
import VoteWidget from './VoteWidget';
import { AuthContext } from '../context/AuthContext.jsx';
import { ToastContext } from '../context/ToastContext.jsx';
import { PATHS } from '../constants/constants';

const PostCard = ({ postBody, fullMode = false, showForumName = false }) => {
    const [postOptions, setPostOptions] = useState(false);
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [userVote, setUserVote] = useState(0);
    const [forumName, setForumName] = useState("");
    const [loading, setLoading] = useState(true);
    const isVotingRef = useRef(false);
    const [commentCount, setCommentCount] = useState(null);
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);
    const navigate = useNavigate();
    const menuRef = useRef(null);

    // Listen for comment count update events
    useEffect(() => {
        function handleCommentCountUpdate(e) {
            if (e.detail && (e.detail.postId === postBody.id || e.detail.postId === postBody.postId)) {
                const pid = e.detail.postId;
                const n = Number(e.detail.commentCount);
                const sanitized = Number.isFinite(n) ? Math.max(0, Math.trunc(n)) : 0;
                console.debug('[PostCard] received postCommentCountUpdated event', { pid, raw: e.detail.commentCount, sanitized });
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
    }, [postBody.commentCount]);

    const formattedTime = postBody.createdAt ? formatDistanceToNow(new Date(postBody.createdAt), { addSuffix: true }) : 'Recently';

    const navigateToPost = () => {
        if(fullMode)return;
        try {
            sessionStorage.setItem(`CINEMATE_LAST_POST_${postBody.id}`, JSON.stringify(postBody));
        } catch (e) {
            // ignore storage errors
        }navigate(PATHS.POST.FULLPAGE(postBody.id));
    };

    const handleDelete = async () => {
        const result = await Swal.fire({
            title: 'Delete Post?',
            text: 'Are you sure you want to delete this post?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, delete',
            confirmButtonColor: '#d33',
            cancelButtonText: 'Cancel',

        });

        if (!result.isConfirmed) {
            try { showToast('', 'Delete cancelled', 'info'); } catch (e) {}
            return;
        }

        setPostOptions(false);

        try{
            const res = await deletePostApi({
                postId: postBody.id
            });

            if(res.success){
                try { showToast('', 'Post deleted', 'success'); } catch (e) {}
                console.log('Post deleted successfully');
                navigate(`/forum/${postBody.forumId}`);
            }

            else{
                if (res.success === false) return showToast('Failed to delete post', res.message || 'unknown error', 'error')
            }
        }
        catch(error){
            return showToast('Failed to delete post', error || 'unknown error', 'error')
        } 

    }

    const handleEdit = () => {
        if(fullMode){
            navigate('.', { state: { editMode: true }, replace: true });
            return;
        }
        setPostOptions(false);
        navigate(PATHS.POST.FULLPAGE(postBody.id), { state: { post: postBody, editMode: true } });
    };

    const viewerMenu = [
        { label: "Follow", onClick: () => console.log("Follow clicked") }
    ];
    
    const authorMenu = [
        { label: "Edit", onClick: handleEdit },
        { label: "Delete", onClick: handleDelete }
    ]

    useEffect(() => {
        const initializePost = async () => {
            try{
                const requests = [
                    getModApi({userId: postBody.ownerId}),
                ];
                if(showForumName){
                    requests.push(getForumNameApi({forumId: postBody.forumId}));
                }
                if(user?.id){
                    requests.push(isVotedPostApi({ targetId: postBody.id }));
                }

                const results = await Promise.all(requests);

                if (results[0]?.data) {
                    setFirstName(results[0].data);
                }
    
                if (showForumName && postBody.forumId) {
                    if (results[1]?.data) {
                        setForumName(results[1].data);
                    }
                }
    
                if (user?.id && results[2]?.success) {
                    setUserVote(results[2].data);
                }
            setLoading(false);
            }
            catch (error) {
                showToast('Failed to fetch post', error || 'unknown error', 'error')
                setLoading(false);
            }
        }

        initializePost();
    }, [postBody]);

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


    const ownerIdConverted = postBody.ownerId ? parseInt(postBody.ownerId, 10) : null;


    return(
        <article className="post-card">
            <div className="post-header">
                <div className="user-profile-pic" onClick={() => {navigate(PATHS.USER.PROFILE(ownerIdConverted))}}>
                    {postBody.avatar ? postBody.avatar : <IoIosPerson />}
                </div>
                
                <div className="user-info">
                    {showForumName && (
                    <p className="forum-name" onClick={() => {navigate(PATHS.FORUM.PAGE(postBody.forumId))}} >
                        {loading ? "Loading..." : forumName}
                    </p>
                    )}
                    <Link className="user-name" style={{ fontSize : showForumName ? "14px" : "18px"}} to={PATHS.USER.PROFILE(ownerIdConverted)}> {loading ? "Loading..." : firstName} </Link>
                    <time dateTime={postBody.createdAt} className="post-time" >{formattedTime}</time>
                </div>
                <div className="post-settings" ref={menuRef}>
                    {ownerIdConverted === user.id && (
                        <>
                            <BsThreeDots onClick={() => setPostOptions(prev => !prev)}/>
                            {postOptions && (
                                <div className="options-menu">
                                <ul>
                                {(ownerIdConverted === user.id ? authorMenu : viewerMenu).map((item, index) => (
                                    <li key={index} onClick={item.onClick}>{item.label}</li>
                                ))}
                                </ul>
                            </div>
                            )}
                        </>
                    )}
                    
                </div>
            </div>
            <div className="post-content" onClick={navigateToPost}>
                <div className="post-title" onClick={navigateToPost}>
                    <p>{postBody.title}</p>
                </div>
                <div className="post-media" onClick={navigateToPost}>
                    {postBody.content && <p className="post-text">{postBody.content}</p>}
                    {postBody.media && <img src={postBody.media} alt={postBody.title || "Post content"} />}
                </div>
            </div>
            <footer className="post-footer">
                <VoteWidget
                    targetId={postBody.id}
                    initialUp={postBody.upvoteCount}
                    initialDown={postBody.downvoteCount}
                    isPost={true}
                />
                <div className="post-comment" onClick={navigateToPost}>
                    <FaRegComment />
                    <span className="comment-count">{commentCount !== null ? commentCount : (postBody.commentCount || 0)}</span>
                </div>
                {/* <div className="post-share">
                    <RiShareForwardLine />
                </div> */}
            </footer>
        </article>
    );
};

export default PostCard;