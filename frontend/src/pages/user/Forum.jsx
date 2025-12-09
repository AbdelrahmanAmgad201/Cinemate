import "./style/Forum.css"
import {Link, useLocation, useNavigate, useParams} from "react-router-dom";
import PostCard from "../../components/PostCard.jsx";
import React, {useContext, useState} from "react";
import {IoIosArrowDown, IoIosPerson} from "react-icons/io";
import pic from "../../assets/action.jpg";
import calendar from "../../assets/icons/calendar.png";
import {PATHS} from "../../constants/constants.jsx";
import {formatCount} from "../../utils/formate.jsx";
import {AuthContext} from "../../context/AuthContext.jsx";
import {BsClockHistory, BsFire, BsGraphUp, BsStars} from "react-icons/bs";

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
    { id: 102, username: "DirectorX", avatar: "https://i.pravatar.cc/150?img=12" },
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
    const { forumId } = useParams();
    const [posts, setPosts] = useState(MockPosts);
    const [forumName, setForumName] = useState("unique_forum_name");
    const [forumDescription, setForumDescription] = useState("forum_description");
    const [forumCreationDate, setForumCreationDate] = useState("Mar 17, 2010");
    const [followersCount, setFollowersCount] = useState(11145289);
    const [postsCount, setPostsCount] = useState(123456);

    const [moderators, setModerators] = useState(MOCK_MODS)

    const { user } = useContext(AuthContext);
    // determine if current user is a moderator by checking their id in the moderators list
    const isMod = !!(user && moderators.some((m) => m.id === user.id));
    const [isJoined, setIsJoined] = useState(false); // TODO: handle this properly
    const toggleJoin = () => { // TODO: handle this properly
        setIsJoined(!isJoined);
    }

    const [activeSort, setActiveSort] = useState(SORT_OPTIONS[0]);
    const [isSortOpen, setIsSortOpen] = useState(false);

    return (
        <div className="forum-container">

            {/* Header*/}
            <div className="forum-banner"></div>
            <div className="forum-header">
                <div className="header-left">
                    <div className="forum-icon-placeholder"></div>
                    <h1>{forumName}</h1>
                </div>

                <div className="header-right">
                    <button className="btn btn-outline">+ Create Post</button>
                    {!isMod ? <button className="btn btn-fill" onClick={toggleJoin}>{isJoined ? "Leave" : "Join"}</button>
                        : <button className="btn btn-fill">Mod Tools</button>}
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