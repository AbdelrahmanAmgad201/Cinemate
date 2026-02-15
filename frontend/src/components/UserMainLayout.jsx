import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { BsLayoutSidebar, BsLayoutSidebarInset } from "react-icons/bs";

import NavBar from './NavBar';
import SideBar from './SideBar';

import './style/UserMainLayout.css';
import {SESSION_STORAGE} from "../constants/constants.jsx";


export default function UserMainLayout() {

    // Save sidebar state in session storage
    const [sidebarOpen, setSidebarOpen] = useState(() => {
        try {
            const savedState = sessionStorage.getItem(SESSION_STORAGE.USER_SIDE_BAR_NAME);

            if (savedState != null) return JSON.parse(savedState);

            return true;

        } catch (e) {
            console.error("Error reading session storage", e);
            return true;
        }
    });

    useEffect(() => {
        sessionStorage.setItem(
            SESSION_STORAGE.USER_SIDE_BAR_NAME,
            JSON.stringify(sidebarOpen)
        );
    }, [sidebarOpen]);

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