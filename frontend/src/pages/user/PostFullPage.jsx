import { useState, useEffect, useRef, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import pic from "../../assets/action.jpg";
import { AuthContext } from '../../context/AuthContext';
import EditPost from '../../components/EditPost';
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
        userId: 1,
        avatar: <IoIosPerson />,
        firstName: "Sam",
        lastName: "Jonas",
        time: "22-11-2025",
        title: "This show deserves more recognition",
        media: pic,
        text: `Just finished binge-watching the entire season and I'm blown away. The character development, the cinematography, the soundtrack - everything was perfect.

I can't believe they haven't announced a second season yet. The cliffhanger ending left so many questions unanswered. Anyone else feel the same way?`,
        votes: 1234,
        postId: 1
    },
    { 
        userId: 2,
        firstName: "Jane",
        lastName: "Doe",
        time: "08-12-2024",
        title: "The cinematography in this scene is absolutely stunning",
        media: pic,
        text: `The way they used lighting and color grading here is masterful. You can feel the emotion without a single word being spoken. This is what visual storytelling is all about.`,
        votes: 543,
        postId: 2
    },
    { 
        userId: 3,
        firstName: "John",
        lastName: "Smith",
        time: "09-12-2024",
        text: `After watching countless films this year, I've finally compiled my top 10 list. These movies really stood out for their storytelling, performances, and overall impact.

Would love to hear what made your list this year! Any hidden gems I should check out?`,
        title: "My Top 10 Movies of 2024",
        votes: 892,
        postId: 3
    },
    { 
        userId: 4,
        firstName: "Emily",
        lastName: "Chen",
        time: "10-12-2024",
        title: "Unpopular opinion: The ending was perfect",
        text: `I know a lot of people were disappointed, but I think the ambiguous ending was exactly what the story needed. Not everything needs to be wrapped up in a neat bow.

It leaves room for interpretation and gives us something to think about long after the credits roll. That's the mark of great storytelling.`,
        votes: 267,
        postId: 4
    },
    { 
        userId: 5,
        firstName: "Marcus",
        lastName: "Williams",
        time: "11-12-2024",
        title: "Found this gem at a thrift store today",
        media: pic,
        text: `Can't believe I found the original poster in mint condition! This movie has been my comfort watch for years. Sometimes the best finds are completely unexpected.`,
        votes: 1567,
        postId: 5
    }
    ];

    const [post, setPost] = useState(null);
    const [openImage, setOpenImage] = useState(false);
    const [userVote, setUserVote] = useState(0);
    const [voteCount, setVoteCount] = useState(0);
    const [editMode, setEditMode] = useState(false);
    const [sort, setSort] = useState("best");
    const [commentText, setCommentText] = useState("");
    const [postOptions, setPostOptions] = useState(false);

    const handleVote = (voteType) => {
        const previousVote = userVote;
        const newVote = userVote === voteType ? 0 : voteType;
        
        const voteDifference = newVote - previousVote;
        
        setUserVote(newVote);
        setVoteCount(prevCount => prevCount + voteDifference);
    };

    const handleEdit = () => {
        setEditMode(true);
        setPostOptions(false);
    };

    const canselEdit = () => {
        setEditMode(false);
    };

    const saveEdit = (updatedPost, mediaFile) => {
        setPost(updatedPost);
        setEditMode(false);
        console.log('Post updated:', updatedPost, mediaFile);
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
        { label: "Delete", onClick: () => console.log("Delete clicked") }
    ];

    if (!post) {
        return <div>Loading...</div>;
    }

    if(editMode){
        return (
            <div className="post-page">
                <EditPost post={post} onSave={saveEdit} onCancel={canselEdit} />
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
                        {post.media && <img src={post.media} alt={post.title || "Post content"} onClick={() => setOpenImage(true)}/>}
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