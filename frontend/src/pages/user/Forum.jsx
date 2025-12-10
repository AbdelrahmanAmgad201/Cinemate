import "./style/Forum.css"
import '../../style/CommonModal.css'
import {BsClockHistory, BsFire, BsGraphUp, BsStars} from "react-icons/bs";
import {IoIosArrowDown, IoIosPerson} from "react-icons/io";
import pic from "../../assets/action.jpg";
import calendar from "../../assets/icons/calendar.png";

import {Link, useLocation, useNavigate, useParams} from "react-router-dom";
import PostCard from "../../components/PostCard.jsx";
import React, {useContext, useEffect, useState} from "react";
import {MAX_LENGTHS, PATHS} from "../../constants/constants.jsx";
import {formatCount} from "../../utils/formate.jsx";
import {checkFollowApi, followForumApi, unfollowForumApi, createForumApi} from "../../api/forum-api.jsx";

import {AuthContext} from "../../context/AuthContext.jsx";
import {ToastContext} from "../../context/ToastContext.jsx";

const MockPosts =[
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
]

// TODO: fetch from backend and handle the format
const MOCK_MODS = [
    { id: 101, username: "FilmBuff_99", avatar: null }, // Avatar null will use placeholder
    { id: 10, username: "DirectorX", avatar: "https://i.pravatar.cc/150?img=12" },
    { id: 103, username: "CinemaSins", avatar: "https://i.pravatar.cc/150?img=33" },
];

// TODO: fetch from backend and handle the format
const SORT_OPTIONS = [
    { label: "New", icon: <BsClockHistory />, value: "new" },
    { label: "Hot", icon: <BsFire />, value: "hot" },
    { label: "Top", icon: <BsGraphUp />, value: "top" },
    { label: "Best", icon: <BsStars />, value: "best" },
];

export default function Forum() {

    const { showToast } = useContext(ToastContext);
    const { user } = useContext(AuthContext);
    const { forumId } = useParams();
    const navigate = useNavigate();


    const [posts, setPosts] = useState(MockPosts);
    const [forumName, setForumName] = useState("unique_forum_name");
    const [forumDescription, setForumDescription] = useState("forum_description");
    const [forumCreationDate, setForumCreationDate] = useState("Mar 17, 2010");
    const [followersCount, setFollowersCount] = useState(11145289);
    const [postsCount, setPostsCount] = useState(123456);

    const [moderators, setModerators] = useState(MOCK_MODS)


    // determine if current user is a moderator by checking their id in the moderators list
    const isMod = !!(user && moderators.some((m) => m.id === user.id));

    // TODO: handle this properly
    const [isJoined, setIsJoined] = useState( false);

    useEffect(() => {
        const checkStatus = async () => {
            try {
                const res = await checkFollowApi({ forumId });
                console.log(res)

                if (res && res.success) {
                    setIsJoined(true);
                }
            } catch (error) {
                console.error("Failed to check join status", error);
            }
        };
        if (forumId) {
            checkStatus();
        }
    }, [forumId]);

    const handleJoin = async () => { // TODO: handle this properly
        // Tested the connection
        // const res2 = await createForumApi({name:"HelloWorld", description:"Description"});
        // return;

        if (isJoined) {
            // Tested the connection
            const res = await unfollowForumApi({forumId:'6939a6a548ef866c970381de'});
            console.log(res)
            if (res.success === true) {
                showToast("Success", "You have left the forum.", "success")
                setIsJoined(!isJoined)
                return;
            }

            showToast("Failed to leave forum", res.message || "unknown error", "error")
            return
        }

        // Tested the connection

        const res = await followForumApi({forumId: '6939a6a548ef866c970381de'});
        console.log(res)
        if (res.success === true) {
            showToast("Success", "You have joined the forum.", "success")
            setIsJoined(!isJoined)
            return;
        }

        showToast("Failed to join forum", res.message || "unknown error", "error")
    }

    const [activeSort, setActiveSort] = useState(SORT_OPTIONS[0]);
    const [isSortOpen, setIsSortOpen] = useState(false);

    const [showForm, setShowForm] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [postTitle, setPostTitle] = useState("");
    const [postText, setPostText] = useState("");
    const [postMedia, setPostMedia] = useState("");

    const handleAddPost = async (e) => {
        e.preventDefault();

        setSubmitting(true);

        // TODO: send to backend
        console.log({forumId, postTitle, postText, postMedia, userId: user.id})
        // const res = await

        // if (res.success === true) {
        //     showToast("Success", "Your post has been submitted.", "success")
        // }
        // else {
        //     showToast("Failed to submit review", res.message || "unknown error", "error")
        // }


        setPostTitle("");
        setPostText("");
        setPostMedia("");
        setShowForm(false);
        setSubmitting(false);
    }

    return (
        <div className="forum-container">

            {/* Create post form */}
            {showForm && (
                <div className="modal-overlay" onMouseDown={() => setShowForm(false)}>
                    <div className="modal" onMouseDown={(e) => e.stopPropagation()}>
                        <h3>Create post</h3>
                        <div className="header-left" style={{alignItems: "center", marginBottom: "10px"}}>
                            <div className="forum-icon-placeholder" style={{width: "50px", height: "50px"}}></div>
                            <h1 style={{fontSize: "16px"}}>{forumName}</h1>
                        </div>
                        <form onSubmit={handleAddPost}>

                            <label>
                                Title
                                <textarea
                                    rows="3"
                                    value={postTitle} onChange={e => {
                                    const inputValue = e.target.value;
                                    if (inputValue.length <= MAX_LENGTHS.TEXTAREA) {
                                        setPostTitle(inputValue);
                                    }
                                }}
                                    placeholder={`Title (max ${MAX_LENGTHS.TEXTAREA} characters)`}
                                    maxLength={MAX_LENGTHS.TEXTAREA}
                                    required
                                />
                                <small>{postTitle.length} / {MAX_LENGTHS.TEXTAREA} characters</small>
                            </label>

                            <label>
                                Body
                                <textarea
                                    rows="5"
                                    value={postText} onChange={e => {
                                    const inputValue = e.target.value;
                                    if (inputValue.length <= MAX_LENGTHS.TEXTAREA) {
                                        setPostText(inputValue);
                                    }
                                }}
                                    placeholder={`Body text (max ${MAX_LENGTHS.TEXTAREA} characters)`}
                                    maxLength={MAX_LENGTHS.TEXTAREA}
                                />
                                <small>{postText.length} / {MAX_LENGTHS.TEXTAREA} characters</small>
                            </label>

                            <div className="modal-actions">
                                <button type="button" className="modal-btn-cancel" onClick={() => setShowForm(false)}>Cancel</button>
                                <button type="submit" className="modal-btn-submit" disabled={submitting}>{submitting ? "Adding..." : "Add Post"}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}


            {/* Header*/}
            <div className="forum-banner"></div>
            <div className="forum-header">
                <div className="header-left">
                    <div className="forum-icon-placeholder"></div>
                    <h1>{forumName}</h1>
                </div>

                <div className="header-right">
                    <button className="btn btn-outline" onClick={() => setShowForm(true)}>+  Create Post</button>
                    {!isMod ? <button className="btn btn-fill" onClick={handleJoin}>{isJoined ? "Leave" : "Join"}</button>
                        : <button className="btn btn-fill" onClick={() => navigate(PATHS.MOD.PAGE(forumId))}>Mod Tools</button>}
                </div>
            </div>

            {/* Main Grid -> 2 Cols*/}
            <div className="forum-main-grid">
                <main className="feed-col">
                    <div className="feed-sort-bar">
                        <span className="sort-label">Sort By:</span>

                        <div className="sort-dropdown-container">
                            <button
                                className="sort-trigger"
                                onClick={() => setIsSortOpen(!isSortOpen)}
                                onBlur={() => setTimeout(() => setIsSortOpen(false), 200)} // Close when clicking away
                            >
                                <span className="sort-icon">{activeSort.icon}</span>
                                <span className="sort-text">{activeSort.label}</span>
                                <IoIosArrowDown className={`sort-arrow ${isSortOpen ? 'open' : ''}`} />
                            </button>

                            {isSortOpen && (
                                <div className="sort-dropdown-menu">
                                    {SORT_OPTIONS.map((option) => (
                                        <div
                                            key={option.value}
                                            className={`sort-option ${activeSort.value === option.value ? 'selected' : ''}`}
                                            onClick={() => {
                                                setActiveSort(option);
                                                setIsSortOpen(false);
                                            }}
                                        >
                                            <span className="sort-option-icon">{option.icon}</span>
                                            {option.label}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="posts-list">
                        {posts.map((post, index) => (
                            <PostCard key={index} postBody={post} />
                        ))}
                    </div>

                </main>

                <aside className="sidebar-col">
                    <div className="sidebar-card">
                        <h2 className="sidebar-title">{forumName}</h2>
                        <p className="sidebar-desc">{forumDescription}</p>

                        <span className="sidebar-meta">
                            <img src={calendar} alt="calendar" className="sidebar-meta-icon"/>
                            Created {forumCreationDate}
                        </span>

                        <hr className="sidebar-divider"/>

                        <div className="sidebar-stats row">
                            <div className="stat-box">
                                <span className="stat-num">{formatCount(followersCount)}</span>
                                <span className="stat-label">Followers</span>
                            </div>
                            <div className="stat-box">
                                <span className="stat-num">{formatCount(postsCount)}</span>
                                <span className="stat-label">Posts</span>
                            </div>
                        </div>

                        <hr className="sidebar-divider"/>

                        <div className="sidebar-mods">
                            <h3>MODERATORS</h3>
                            {moderators.map(({ id, username, avatar }) => (
                                <div className="mod-user" key={id}>
                                    <Link to={PATHS.USER.PROFILE(id)}>
                                        <img className="mod-icon-small"
                                             src={avatar}
                                             alt={``}
                                             loading="lazy"
                                        />
                                    </Link>
                                    <span>
                                        <Link to={PATHS.USER.PROFILE(id)}>{username}</Link>
                                        {user && user.id === id && <span className="mod-badge">you</span>}
                                    </span>
                                </div>
                            ))}
                        </div>

                    </div>
                </aside>
            </div>

        </div>
    )
}