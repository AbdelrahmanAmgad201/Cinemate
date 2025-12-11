import React from 'react';
import "./style/SideBar.css"
import {createForumApi} from "../api/forum-api.jsx";

const SideBar = ({ collapsed }) => {
    const addForum = async () => {
        const res = createForumApi({name: "Test 2", description: "Test Desc 2"});
        console.log(res);
    }
    return (
        <aside className={`user-left-sidebar ${collapsed ? 'collapsed' : ''}`}>
            <ul>
                <li>Home</li>
                <li>Popular</li>
                <li>r/Movies</li>
                <li>r/Gaming</li>
            </ul>
            <button onClick={addForum}>Create Forum</button>
        </aside>
    );
};

export default SideBar;