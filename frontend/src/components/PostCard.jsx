import { useState, useEffect, useRef, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { IoIosPerson } from "react-icons/io";
import { BsThreeDots } from "react-icons/bs";
import { BiUpvote, BiDownvote, BiSolidUpvote, BiSolidDownvote } from "react-icons/bi";
import { RiShareForwardLine } from "react-icons/ri";
import { FaRegComment } from "react-icons/fa";
import "./style/postCard.css";
import { AuthContext } from '../context/AuthContext';
import { PATHS } from '../constants/constants';

const PostCard = ({ postBody = {} }) => {
    const [userVote, setUserVote] = useState(0);
    const [voteCount, setVoteCount] = useState(postBody.votes || 0);
    const [postOptions, setPostOptions] = useState(false);
    const [viewerMenu, setViewerMenu] = useState([
        { label: "Follow", onClick: () => console.log("Follow clicked") }
    ]);

    const [authorMenu, setAuthorMenu] = useState([
        { label: "Edit", onClick: () => console.log("Edit clicked") },
        { label: "Delete", onClick: () => console.log("Delete clicked") }
    ]);

    
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const menuRef = useRef(null);

    const handleVote = (voteType) => {
        const previousVote = userVote;
        const newVote = userVote === voteType ? 0 : voteType;
        
        const voteDifference = newVote - previousVote;
        
        setUserVote(newVote);
        setVoteCount(prevCount => prevCount + voteDifference);
    };

    const handleOptions = () => {
        setPostOptions(!postOptions);
    };

    const navigateToPost = () => {
        navigate(PATHS.POST.FULLPAGE(postBody.postId));
    }

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

    return(
        <article className="post-card">
            <div className="post-header">
                <div className="user-profile-pic">
                    {postBody.avatar ? postBody.avatar : <IoIosPerson />}
                </div>
                <div className="user-info">
                    <h2 className="user-name">{postBody.firstName} {postBody.lastName}</h2>
                    <time dateTime="2024-12-09 30:00">{postBody.time}</time>
                </div>
                <div className="post-settings" ref={menuRef}>
                    <BsThreeDots onClick={() => setPostOptions(prev => !prev)}/>
                        {postOptions && (
                            <div className="options-menu">
                            <ul>
                            {(postBody.userId === user.id ? authorMenu : viewerMenu).map((item, index) => (
                                <li key={index} onClick={item.onClick}>{item.label}</li>
                            ))}
                            </ul>
                        </div>
                        )}
                </div>
            </div>
            <div className="post-content">
                <div className="post-title" onClick={navigateToPost}>
                    <p>{postBody.title}</p>
                </div>
                <div className="post-media" onClick={navigateToPost}>
                    {postBody.media ?
                    (<img src={postBody.media} alt={postBody.title || "Post content"} />)
                    : 
                    (null)
                    }
                    
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
                <div className="post-share">
                    <RiShareForwardLine />
                </div>
            </footer>
        </article>
    );
};

export default PostCard;