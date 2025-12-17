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
import { deletePostApi,isVotedPostApi, deleteVotePostApi, votePostApi, updateVotePostApi } from '../api/post-api.jsx';
import { getModApi } from '../api/forum-api.jsx';
import { AuthContext } from '../context/AuthContext.jsx';
import { ToastContext } from '../context/ToastContext.jsx';
import { PATHS } from '../constants/constants';

const PostCard = ({ postBody }) => {
    const [userVote, setUserVote] = useState(0);
    const [voteCount, setVoteCount] = useState(postBody?.votes || 0);
    const [postOptions, setPostOptions] = useState(false);
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const isVotingRef = useRef(false);
    
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);
    const navigate = useNavigate();
    const menuRef = useRef(null);

    const formattedTime = postBody.createdAt ? formatDistanceToNow(new Date(postBody.createdAt), { addSuffix: true }) : 'Recently';

    const handleVote = async (voteType) => {
        const previousVote = userVote;
        const newVote = userVote === voteType ? 0 : voteType;
        
        const voteDifference = newVote - previousVote;
        
        setUserVote(newVote);
        setVoteCount(prevCount => prevCount + voteDifference);

        try{
            let result;
            if(previousVote === 0 && newVote !== 0){
                result = await votePostApi({ postId: postBody.id, value: newVote });
                if (result.success) {
                    console.log("Vote created");
                }
            }
            else if (newVote === 0 && previousVote !== 0){
                result = await deleteVotePostApi({ targetId: postBody.id });
                
                
                if (result.success) {
                    console.log("Vote deleted");
                }
            }

            else if (previousVote !== 0 && newVote !== 0) {
                result = await updateVotePostApi({ postId: postBody.id, value: newVote });
            }
            if (!result?.success) {
                setUserVote(previousVote);
                setVoteCount(prevCount => prevCount - voteDifference);
                showToast('Failed to vote', result.message || 'unknown error', 'error')
            }
        }
        catch(error){
            setUserVote(previousVote);
            setVoteCount(prevCount => prevCount - voteDifference);
            showToast('Failed to vote', error || 'unknown error', 'error')
        }
    };

    const navigateToPost = () => {
        navigate(PATHS.POST.FULLPAGE(postBody.id), {state: {post: postBody}});
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

        if (!result.isConfirmed) return;

        setPostOptions(false);

        try{
            const res = await deletePostApi({
                postId: postBody.id
            });

            if(res.success){
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
        const checkVote = async () => {
            if (!postBody?.id || !user?.id || isVotingRef.current) {
                return;
            }

            try {
                const result = await isVotedPostApi({ targetId: postBody.id });
                console.log("isVoted", result);
                if(result.success) {
                    setUserVote(result.data);
                } else {
                    showToast('Failed to fetch votes', result.message || 'unknown error', 'error')
                    setUserVote(0);
                }
            } 
            catch(error){
                showToast('Failed to fetch votes', error || 'unknown error', 'error')
                setUserVote(0);
            }
        }

        const getUsername = async () => {
            const result = await getModApi({userId: postBody.ownerId});
            setFirstName(result.data);
        }


        checkVote();
        getUsername();
    }, [postBody?.ownerId, user?.id]);

    useEffect(() => {
        if (postBody) {
            const totalVotes = (postBody.upvoteCount || 0) - (postBody.downvoteCount || 0);
            setVoteCount(totalVotes);
        }
    }, [postBody?.upvoteCount, postBody?.downvoteCount]);

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
                    <Link className="user-name" to={PATHS.USER.PROFILE(ownerIdConverted)} >{firstName} {/*lastName*/}</Link>
                    <time dateTime={postBody.createdAt} >{formattedTime}</time>
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
                <div className="post-comment" onClick={navigateToPost}>
                    <FaRegComment />
                </div>
                {/* <div className="post-share">
                    <RiShareForwardLine />
                </div> */}
            </footer>
        </article>
    );
};

export default PostCard;