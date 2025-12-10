import React, { useContext, useState } from "react";
import { AuthContext } from "../../context/AuthContext.jsx";
import {useNavigate, useParams} from "react-router-dom";
import { ToastContext } from "../../context/ToastContext.jsx";
import Swal from "sweetalert2";
import './style/Mod.css';
import PostCard from "../../components/PostCard.jsx";
import {PATHS} from "../../constants/constants.jsx";
import {IoIosPerson} from "react-icons/io";
import pic from "../../assets/action.jpg";
import {updateForumApi, deleteForumApi, getForumApi} from "../../api/forum-api.jsx";

//TODO: Fetch forum data from backend
// Maybe fetch posts? i dont see a need for this to be in mod page


const MOCK_POSTS = [
    {
        postId: 1,
        userId: 10,
        title: "Wish There Was A Second Season",
        firstName: "Sam",
        lastName: "Jonas",
        time: "22-11-2025",
        avatar: <IoIosPerson />, // Add avatar
        media: pic, // Add media
        votes: 1234,
        text: "This is the post content..."
    },
    {
        postId: 2,
        userId: 11,
        title: "I liked This Scene A Lot",
        firstName: "Jane",
        lastName: "Doe",
        time: "08-12-2024",
        avatar: <IoIosPerson />,
        media: null,
        votes: 543,
        text: "Another post text..."
    },
];

const MOCK_MODS = [
    { id: 101, username: "FilmBuff_99", avatar: null },
    { id: 1, username: "DirectorX", avatar: "https://i.pravatar.cc/150?img=12" },
    { id: 103, username: "CinemaSins", avatar: "https://i.pravatar.cc/150?img=33" },
];

export default function Mod() {
    const { user } = useContext(AuthContext);
    const { showToast } = useContext(ToastContext);
    const navigate = useNavigate();
    const { forumId } = useParams();

    const isMod = !!(user && MOCK_MODS.some((m) => m.id === user.id));

    const [forumName, setForumName] = useState("unique_forum_name");
    const [forumDescription, setForumDescription] = useState("forum_description");
    const [posts, setPosts] = useState(MOCK_POSTS);
    const [saving, setSaving] = useState(false);
    const [deleted, setDeleted] = useState(false);

    // Tested connection to backend
    const handleSave = async (e) => {
        e.preventDefault();
        const result = await Swal.fire({
            title: 'Update forum?',
            text: 'Are you sure you want to update the forum data?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, update',
            cancelButtonText: 'Cancel',

        });

        if (!result.isConfirmed) return;

        setSaving(true);

        const res = await updateForumApi({forumId, name: forumName, description: forumDescription});
        console.log({res, forumId: forumId, user: user.id, isMod: isMod})

        setSaving(false);

        if (res.success === false) return showToast('Failed to update forum', res.message || 'unknown error', 'error')

        showToast('Saved', 'Forum data updated successfully', 'success');
    };

    // Tested connection to backend
    const handleDeleteForum = async () => {
        const result = await Swal.fire({
            title: 'Delete forum?',
            text: 'Are you sure you want to DELETE this forum? This cannot be undone.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, delete',
            confirmButtonColor: '#d33',
            cancelButtonText: 'Cancel',

        });

        if (!result.isConfirmed) return;

        const res = await deleteForumApi({forumId});
        console.log({res, forumId: forumId, user: user.id, isMod: isMod})

        setDeleted(true);

        if (res.success === false) return showToast('Failed to delete forum', res.message || 'unknown error', 'error')

        showToast('Deleted', 'Forum marked deleted', 'info');
    };

    const handleDeletePost = async (e, postId) => {
        e.stopPropagation();
        const result = await Swal.fire({
            title: 'Delete post?',
            text: 'Are you sure you want to delete this post?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, delete',
            confirmButtonColor: '#d33',
            cancelButtonText: 'Cancel',

        });

        if (!result.isConfirmed) return;

        // Refetch posts
        showToast('Deleted', 'Post removed', 'info');
    };

    const handlePostClick = (postId) => {
        // Navigate to the post full page
        navigate(PATHS.POST.FULLPAGE(postId));
    };

    if (deleted) return <div className="mod-container"><h2>Forum deleted</h2></div>;

    if (!isMod) return <div className="mod-container"><h2>You are not a moderator</h2></div>;

    return (
        <div className="mod-container">



            <h1 className="mod-page-title">Moderator Tools</h1>

            <div className="header-left" style={{alignItems: "center", marginBottom: "10px"}}>
                <div className="forum-icon-placeholder" style={{width: "50px", height: "50px"}}></div>
                <h1 style={{fontSize: "16px"}}>{forumName}</h1>
            </div>

            <section className="mod-section">
                <h2 className="section-title">Forum Settings</h2>
                <form onSubmit={handleSave}>
                    <div className="form-group">
                        <label className="form-label">Forum Name</label>
                        <input
                            className="form-input"
                            value={forumName}
                            onChange={(e) => setForumName(e.target.value)}
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Description</label>
                        <textarea
                            className="form-input"
                            value={forumDescription}
                            onChange={(e) => setForumDescription(e.target.value)}
                            rows={4}
                        />
                    </div>
                    <div className="form-actions">
                        <button className="btn btn-fill" type="submit" disabled={saving}>
                            {saving ? 'Saving...' : 'Save Changes'}
                        </button>
                        <button
                            type="button"
                            className="btn btn-delete"
                            onClick={handleDeleteForum}
                        >
                            Delete Forum
                        </button>
                    </div>
                </form>
            </section>



            {/*{posts.length === 0 && <p className="empty-state">No posts found in this forum.</p>}*/}

            {/*<section className="mod-section">*/}
            {/*    <h2 className="section-title">Manage Posts ({posts.length})</h2>*/}

            {/*    {posts.length === 0 && <p className="empty-state">No posts found.</p>}*/}

            {/*    <div className="mod-post-list">*/}
            {/*        {posts.map((p) => (*/}
            {/*            <div*/}
            {/*                key={p.postId}*/}
            {/*                className="mod-post-wrapper"*/}
            {/*                onClick={() => handlePostClick(p.postId)} // Clicking anywhere on wrapper goes to post*/}
            {/*            >*/}
            {/*                /!* The Actual Post Card *!/*/}
            {/*                <div className="mod-card-scale">*/}
            {/*                    <PostCard postBody={p} />*/}
            {/*                </div>*/}

            {/*                /!* The Overlay Actions *!/*/}
            {/*                <div className="mod-post-overlay">*/}
            {/*                    <span className="mod-overlay-label">Post #{p.postId}</span>*/}
            {/*                    <button*/}
            {/*                        className="btn btn-delete btn-sm"*/}
            {/*                        onClick={(e) => handleDeletePost(e, p.postId)} // Pass 'e' to stop propagation*/}
            {/*                    >*/}
            {/*                        Delete Post*/}
            {/*                    </button>*/}
            {/*                </div>*/}
            {/*            </div>*/}
            {/*        ))}*/}
            {/*    </div>*/}
            {/*</section>*/}


        </div>
    );
}