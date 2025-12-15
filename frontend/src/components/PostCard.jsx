import { useState, useEffect, useRef, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { IoIosPerson } from "react-icons/io";
import { BsThreeDots } from "react-icons/bs";
import { BiUpvote, BiDownvote, BiSolidUpvote, BiSolidDownvote } from "react-icons/bi";
import { RiShareForwardLine } from "react-icons/ri";
import { FaRegComment } from "react-icons/fa";
import "./style/postCard.css";
import { deletePostApi } from '../api/post-api.jsx';
import VoteWidget from './VoteWidget';
import { AuthContext } from '../context/AuthContext.jsx';
import { PATHS } from '../constants/constants';

const PostCard = ({ postBody }) => {
    const [postOptions, setPostOptions] = useState(false);
    const isVotingRef = useRef(false);
    const [commentCount, setCommentCount] = useState(postBody.commentCount || 0);
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const menuRef = useRef(null);

    // Listen for comment count update events
    useEffect(() => {
        function handleCommentCountUpdate(e) {
            if (e.detail && (e.detail.postId === postBody.id || e.detail.postId === postBody.postId)) {
                setCommentCount(e.detail.commentCount);
            }
        }
        window.addEventListener('postCommentCountUpdated', handleCommentCountUpdate);
        return () => window.removeEventListener('postCommentCountUpdated', handleCommentCountUpdate);
    }, [postBody.id, postBody.postId]);

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
                <VoteWidget
                    targetId={postBody.id}
                    initialUp={postBody.upvoteCount}
                    initialDown={postBody.downvoteCount}
                    isPost={true}
                />
                <div className="post-comment" onClick={navigateToPost}>
                    <FaRegComment />
                    <span className="comment-count">{commentCount}</span>
                </div>
                {/* <div className="post-share">
                    <RiShareForwardLine />
                </div> */}
            </footer>
        </article>
    );
};

export default PostCard;