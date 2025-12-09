import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { BsLayoutSidebar, BsLayoutSidebarInset } from "react-icons/bs";

import NavBar from './NavBar';
import SideBar from './SideBar';

import './style/UserMainLayout.css';


export default function UserMainLayout() {

    const [sidebarOpen, setSidebarOpen] = useState(true);

    return (
        <div className="user-main-layout-container">
            <NavBar />

            <div className={`user-main-layout-grid ${sidebarOpen ? '' : 'sidebar-closed'}`}>

                <SideBar collapsed={!sidebarOpen} />

                <main className="user-main-content">

                    <div className="layout-header">
                        <button
                            className="sidebar-toggle-btn"
                            onClick={() => setSidebarOpen((s) => !s)}
                            title={sidebarOpen ? "Close Sidebar" : "Open Sidebar"}
                        >
                            {sidebarOpen ? <BsLayoutSidebarInset size={24} /> : <BsLayoutSidebar size={24} />}
                        </button>
                    </div>

                    <div className="content-wrapper">
                        <Outlet />
                    </div>
                </main>
            </div>
        </div>
    )
}