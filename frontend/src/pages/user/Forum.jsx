import "./style/Forum.css"
import '../../style/CommonModal.css'
import {BsClockHistory, BsFire, BsGraphUp, BsStars} from "react-icons/bs";
import {IoIosArrowDown, IoIosPerson} from "react-icons/io";
import pic from "../../assets/action.jpg";
import calendar from "../../assets/icons/calendar.png";

import {Link, useNavigate, useParams} from "react-router-dom";
import React, {useCallback, useContext, useEffect, useState} from "react";
import {MAX_LENGTHS, PATHS, PAGE_SIZE} from "../../constants/constants.jsx";
import {formatCount} from "../../utils/formate.jsx";
import {checkFollowApi, followForumApi, unfollowForumApi, getForumPostsApi, getForumApi, getModApi} from "../../api/forum-api.jsx";
import PostsFeed from "../../components/PostsFeed.jsx";
import  {addPostApi} from "../../api/post-api.jsx";

import {AuthContext} from "../../context/AuthContext.jsx";
import {ToastContext} from "../../context/ToastContext.jsx";


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


    const [posts, setPosts] = useState([]);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true); // if there are no more posts
    const [error, setError] = useState();

    const [forumName, setForumName] = useState();
    const [forumDescription, setForumDescription] = useState();
    const [forumCreationDate, setForumCreationDate] = useState("Mar 17, 2010");
    const [followersCount, setFollowersCount] = useState();
    const [postsCount, setPostsCount] = useState();

    const [moderators, setModerators] = useState([])


    // determine if current user is a moderator by checking their id in the moderators list
    const isMod = !!(user && moderators.some((m) => m.id === user.id));

    // TODO: handle this properly
    const [isJoined, setIsJoined] = useState( false);

    // TODO: handle MOD


    useEffect(() => {

        const fetchForumDetails = async () => {
            const res = await getForumApi({forumId})
            const data = res.data;
            // console.log(data)

            if (!res.success) {
                showToast("Failed to fetch forum details", res.message || "unknown error", "error")
                setError(res.message)
                navigate(PATHS.HOME);
                return;
            }

            setError(null)
            setForumName(data.name)
            setForumDescription(data.description)
            setForumCreationDate(new Date(data.createdAt).toDateString())
            setFollowersCount(data.followerCount)
            setPostsCount(data.postCount)
            const res2 = await getModApi({userId: data.ownerId24Bit})
            setModerators([{id: data.ownerId, username: res2.data, avatar:null}])
        }

        const checkStatus = async () => {
            try {
                const res = await checkFollowApi({ forumId });
                const joined = res.data;
                console.log(res)

                if (res && joined) {
                    setIsJoined(true);
                }
            } catch (error) {
                console.error("Failed to check join status", error);
            }
        };
        if (forumId) {
            fetchForumDetails();
            checkStatus();
        }
    }, [forumId, showToast]);

    const handleJoin = async () => { // TODO: handle this properly
        // Tested the connection

        if (isJoined) {
            // Tested the connection
            const res = await unfollowForumApi({forumId});
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

        const res = await followForumApi({forumId});
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

    // TODO: Test
    const handleAddPost = async (e) => {
        e.preventDefault();

        setSubmitting(true);

        const res = await addPostApi({forumId, title: postTitle, content: postText})

        if (res.success === true) {
            showToast("Success", "Your post has been submitted.", "success")
            setPostTitle("");
            setPostText("");
            setPostMedia("");
            setShowForm(false);
        }
        else {
            showToast("Failed to submit review", res.message || "unknown error", "error")
        }

        setSubmitting(false);
    }

    // we use useCallback to memoize the fetchPosts function so that it doesn't re-render on every render cycle.
    const fetchPosts = useCallback(async (pageNum) => {
        setLoading(true);

        const res = await getForumPostsApi({forumId, page:pageNum, size: PAGE_SIZE.FORUM});
        // console.log(res)
        const newPosts = res.data.posts;
        if (!res.success){
            setHasMore(false);
            return;
        }

        setPosts(prevPosts => {
            if (pageNum === 0) return newPosts;
            return [...prevPosts, ...newPosts];
        });

        if (newPosts.length < PAGE_SIZE.FORUM) setHasMore(false);

        setLoading(false);


    }, [forumId])

    // Initial page load
    useEffect(() => {
        setPosts([])
        setPage(0);
        setHasMore(true);
        fetchPosts(0);
    }, [forumId, fetchPosts])

    const handleLoadMore = () => {
        const nextPage = page + 1;
        setPage(nextPage);
        fetchPosts(nextPage);
    };

    if (error != null) return (
        <div className="forum-container" style={{display: "flex", justifyContent: "center"}}>
            <div className="error-message">No forum exists</div>
        </div>
    )

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

                    <PostsFeed
                        posts={posts}
                        loading={loading}
                        hasMore={hasMore}
                        onLoadMore={handleLoadMore}
                        emptyMessage="No posts here yet."
                    />

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