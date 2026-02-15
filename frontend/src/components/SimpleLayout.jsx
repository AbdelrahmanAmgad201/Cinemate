import React from 'react';
import { Outlet } from 'react-router-dom';
import NavBar from './NavBar';

const SimpleLayout = () => {
    return (
        <div className="simple-layout-container">
            <NavBar />

            <main className="simple-layout-content">
                <Outlet />
            </main>
        </div>
    );
};

export default SimpleLayout;