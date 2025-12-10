import { useState, useEffect, useRef, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import pic from "../assets/action.jpg";
import { AuthContext } from '../context/AuthContext.jsx';
import "./style/postCard.css";
import "./style/postFullPage.css";
import { IoIosPerson } from "react-icons/io";
import { BsThreeDots } from "react-icons/bs";
import { BiUpvote, BiDownvote, BiSolidUpvote, BiSolidDownvote } from "react-icons/bi";
import { RiShareForwardLine } from "react-icons/ri";
import { FaRegComment } from "react-icons/fa";
import { MdKeyboardArrowDown } from "react-icons/md";

const PostFullPage = () => {
    const { postId } = useParams();

    const mockPost = [
        { 
            userId: 1,
            avatar: <IoIosPerson />,
            firstName: "Sam",
            lastName: "Jonas",
            time: "22-11-2025",
            title: "Wish There Was A Second Season",
            media: pic,
            text: "Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. ",
            votes: 1234,
            postId: 1
        },
        { 
            userId: 2,
            firstName: "Jane",
            lastName: "Doe",
            time: "08-12-2024",
            title: "I liked This Scene A Lot",
            media: pic,
            votes: 543,
            postId: 2
        },
        { 
            userId: 3,
            firstName: "John",
            lastName: "Smith",
            time: "09-12-2024",
            text: "Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. Wish There Was A Second Season. ",
            title: "My Top Movies!",
            votes: 892,
            postId: 3
        }
    ];

    const [post, setPost] = useState(null);
    const [userVote, setUserVote] = useState(0);
    const [voteCount, setVoteCount] = useState(0);
    const [sort, setSort] = useState("best");
    const [commentText, setCommentText] = useState("");
    const [postOptions, setPostOptions] = useState(false);

    const viewerMenu = [
        { label: "Follow", onClick: () => console.log("Follow clicked") }
    ];
    
    const authorMenu = [
        { label: "Edit", onClick: () => console.log("Edit clicked") },
        { label: "Delete", onClick: () => console.log("Delete clicked") }
    ];

    
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

    useEffect(() => {
        const foundPost = mockPost.find(p => p.postId === parseInt(postId));
        
        if (foundPost) {
            setPost(foundPost);
            setVoteCount(foundPost.votes || 0);
        } else {
            console.error("Post not found");
        }
    }, [postId]);

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
        return <div>Loading...</div>;
    }

    return (
        <div className="post-page">
            <article className="post-card">
                <div className="post-header">
                    <div className="user-profile-pic">
                        {post.avatar ? post.avatar : <IoIosPerson />}
                    </div>
                    <div className="user-info">
                        <h2 className="user-name">{post.firstName} {post.lastName}</h2>
                        <time dateTime={post.time}>{post.time}</time>
                    </div>
                    <div className="post-settings" ref={menuRef}>
                        <BsThreeDots onClick={() => setPostOptions(prev => !prev)}/>
                            {postOptions && (
                                <div className="options-menu">
                                <ul>
                                {(post.userId === user?.id ? authorMenu : viewerMenu).map((item, index) => (
                                    <li key={index} onClick={item.onClick}>{item.label}</li>
                                ))}
                                </ul>
                            </div>
                            )}
                    </div>
                </div>
                <div className="post-content">
                    <div className="post-title" >
                        <p>{post.title}</p>
                    </div>
                    <div className="post-media" >
                        {post.media && <img src={post.media} alt={post.title || "Post content"} />}
                        {post.text && <p className="post-text">{post.text}</p>}
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
                    <div className="post-share">
                        <RiShareForwardLine />
                    </div>
                </footer>
            </article>
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