import React from 'react';
import "./style/SideBar.css"

const SideBar = ({ collapsed }) => {
    return (
        <aside className={`user-left-sidebar ${collapsed ? 'collapsed' : ''}`}>
            <ul>
                <li>Home</li>
                <li>Popular</li>
                <li>r/Movies</li>
                <li>r/Gaming</li>
            </ul>
        </aside>
    );
};

export default SideBar;