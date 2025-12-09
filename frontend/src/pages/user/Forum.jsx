import "./style/Forum.css"
import {useLocation, useNavigate, useParams} from "react-router-dom";
import PostCard from "../../components/PostCard.jsx";
import React, {useState} from "react";
import {IoIosPerson} from "react-icons/io";
import pic from "../../assets/action.jpg";

const posts =[
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


export default function Forum() {
    const { forumId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();


    return (
        <div className="forum-container">

            {/* Header*/}
            <div className="forum-banner"></div>
            <div className="forum-header">
                <div className="header-left">
                    <div className="forum-icon-placeholder"></div>
                    <h1>unique_forum_name</h1>
                </div>

                <div className="header-right">
                    <button className="btn btn-outline">+ Create Post</button>
                    <button className="btn btn-fill">Join/Mod Tools</button>
                </div>
            </div>

            {/* Main Grid -> 2 Cols*/}
            <div className="forum-main-grid">
                <main className="feed-col">
                    <div className="feed-sort-bar">
                        <span>Sort By: <strong>New</strong> ▼</span>
                    </div>

                    <div className="posts-list">
                        {posts.map((post, index) => (
                            <PostCard key={index} postBody={post} />
                        ))}
                    </div>

                </main>

                <aside className="sidebar-col">
                    <div className="sidebar-card">
                        <h2 className="sidebar-title">unique_forum_name</h2>
                        <p className="sidebar-desc">Forum_description goes here. This is a place for cool movie
                            discussions...
                        </p>

                        <div className="sidebar-meta">
                            📅 Created Mar 17, 2010
                        </div>

                        <hr className="sidebar-divider"/>

                        <div className="sidebar-stats row">
                            <div className="stat-box">
                                <span className="stat-num">187K</span>
                                <span className="stat-label">Followers</span>
                            </div>
                            <div className="stat-box">
                                <span className="stat-num">12k</span>
                                <span className="stat-label">Posts</span>
                            </div>
                        </div>

                        <hr className="sidebar-divider"/>

                        <div className="sidebar-mods">
                            <h3>MODERATOR</h3>
                            <div className="mod-user">
                                <div className="mod-icon-small"></div>
                                <span>Username</span>
                            </div>
                        </div>

                    </div>
                </aside>
            </div>

        </div>
    )
}