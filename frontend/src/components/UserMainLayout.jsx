import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';

import NavBar from './NavBar';
import SideBar from './SideBar';
import IconButton from './ui/IconButton.jsx';

import './style/UserMainLayout.css';
import { SESSION_STORAGE } from '../constants/constants.jsx';

export default function UserMainLayout() {
    const [sidebarOpen, setSidebarOpen] = useState(() => {
        try {
            const savedState = sessionStorage.getItem(SESSION_STORAGE.USER_SIDE_BAR_NAME);
            if (savedState != null) return JSON.parse(savedState);
            return true;
        } catch {
            return true;
        }
    });

    useEffect(() => {
        sessionStorage.setItem(SESSION_STORAGE.USER_SIDE_BAR_NAME, JSON.stringify(sidebarOpen));
    }, [sidebarOpen]);

    return (
        <div className="user-main-layout-container">
            <NavBar />

            <div className={`user-main-layout-grid ${sidebarOpen ? '' : 'sidebar-closed'}`}>
                <SideBar collapsed={!sidebarOpen} />

                <main className="user-main-content">
                    <div className="layout-header">
                        <IconButton
                            label={sidebarOpen ? 'Close sidebar' : 'Open sidebar'}
                            onClick={() => setSidebarOpen((s) => !s)}
                        >
                            {sidebarOpen ? <PanelLeftClose size={19} /> : <PanelLeftOpen size={19} />}
                        </IconButton>
                    </div>

                    <div className="content-wrapper">
                        <Outlet />
                    </div>
                </main>
            </div>
        </div>
    );
}
