import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import "./style/SideBar.css";
import FollowedForums from './FollowedForums.jsx';
import { PATHS } from '../constants/constants.jsx';
import { useSearchParams } from 'react-router-dom';

const SideBar = ({ collapsed }) => {
    const location = useLocation();
    const [searchParams] = useSearchParams();
    const feed = searchParams.get('feed') || 'following';

    const isHomeRoute = location.pathname === PATHS.HOME && (feed === 'following' || !searchParams.get('feed'));
    const isPopular = location.pathname === PATHS.HOME && feed === 'popular';
    const isExplore = location.pathname === PATHS.FORUM.EXPLORE;

    const handleNavClick = () => {
        try {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } catch (e) {
            window.scrollTo(0, 0);
        }
    };

    return (
        <aside className={`user-left-sidebar ${collapsed ? 'collapsed' : ''}`}>
            <ul>
                <li>
                    <Link to={`${PATHS.HOME}`} className={`sidebar-button ${isHomeRoute ? 'active' : ''}`} onClick={handleNavClick}>
                        Home
                    </Link>
                </li>
                <li>
                    <Link to={`${PATHS.HOME}?feed=popular`} className={`sidebar-button ${isPopular ? 'active' : ''}`} onClick={handleNavClick}>
                        Popular
                    </Link>
                </li>
                <li>
                    <Link to={`${PATHS.FORUM.EXPLORE}`} className={`sidebar-button ${isExplore ? 'active' : ''}`} onClick={handleNavClick}>
                        Explore forums
                    </Link>
                </li>
                <li>
                    <FollowedForums />
                </li>
                <li>
                    <div className="sidebar-button">Create a forum</div>
                </li>
            </ul>
        </aside>
    );
};

export default SideBar;