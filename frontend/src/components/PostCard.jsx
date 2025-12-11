import { useState, useEffect, useRef, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { IoIosPerson } from "react-icons/io";
import { BsThreeDots } from "react-icons/bs";
import { BiUpvote, BiDownvote, BiSolidUpvote, BiSolidDownvote } from "react-icons/bi";
import { RiShareForwardLine } from "react-icons/ri";
import { FaRegComment } from "react-icons/fa";
import "./style/postCard.css";
import { deletePostApi,isVotedPostApi, deleteVotePostApi, votePostApi, updateVotePostApi } from '../api/post-api.jsx';
import { AuthContext } from '../context/AuthContext.jsx';
import { PATHS } from '../constants/constants';

const PostCard = ({ postBody }) => {
    const [userVote, setUserVote] = useState(0);
    const [voteCount, setVoteCount] = useState(postBody?.votes || 0);
    const [postOptions, setPostOptions] = useState(false);
    const [voteId, setVoteId] = useState(postBody?.voteId || null);
    const isVotingRef = useRef(false);
    
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const menuRef = useRef(null);

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
                console.error('Vote failed:', result?.message);
            }
        }
        catch(e){
            setUserVote(previousVote);
            setVoteCount(prevCount => prevCount - voteDifference);
            console.error('Vote error:', e);
        }
    };

    const navigateToPost = () => {
        navigate(PATHS.POST.FULLPAGE(postBody.id), {state: {post: postBody}});
    };

    const handleDelete = async () => {
        if (!window.confirm('Are you sure you want to delete this post?')) {
            return;
        }

        setPostOptions(false);

        try{
            const result = await deletePostApi({
                postId: postBody.id
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
        setPostOptions(false);
        navigate(PATHS.POST.FULLPAGE(postBody.id), { state: { post: postBody, editMode: true } });s
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
                
                if (result.success) {
                    let voteValue = 0;
                    if (typeof result.data === 'number') {
                        voteValue = result.data;
                    } else if (typeof result.data === 'boolean') {
                        voteValue = 0;
                    } else if (result.data && typeof result.data.value === 'number') {
                        voteValue = result.data.value;
                    }
                    
                    setUserVote(voteValue);
                } else {
                    console.log("No existing vote found");
                    setUserVote(0);
                }
            } catch (e) {
                console.error('Error checking vote:', e);
                setUserVote(0);
            }
        }

        checkVote();
    }, [postBody?.id, user?.id]);

    useEffect(() => {
        if (postBody) {
            const totalVotes = (postBody.upvoteCount || 0) - (postBody.downvoteCount || 0);
            setVoteCount(totalVotes);
        }
    }, [postBody?.id, postBody?.upvoteCount, postBody?.downvoteCount]);

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
                <div className="user-profile-pic">
                    {postBody.avatar ? postBody.avatar : <IoIosPerson />}
                </div>
                <div className="user-info">
                    <h2 className="user-name">{postBody.firstName} {postBody.lastName}</h2>
                    <time dateTime={postBody.time}>{postBody.time}</time>
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
            <div className="post-content">
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