import { useContext } from 'react';
import { Link, useLocation } from 'react-router-dom';
import ProfileAvatar from './ProfileAvatar';
import { AuthContext } from '../context/AuthContext.jsx';
import '../pages/admin/style/NavBar.css';
import { Film } from 'lucide-react';

import { ROLES, PATHS } from '../constants/constants.jsx';

function NavBar() {
    const { user } = useContext(AuthContext);
    const location = useLocation();

    const tabs = user?.role === ROLES.ADMIN
        ? [
            { key: PATHS.ADMIN.REVIEW_REQUESTS, title: 'Review Requests', to: PATHS.ADMIN.REVIEW_REQUESTS },
            { key: PATHS.ADMIN.SITE_ANALYTICS, title: 'Site Analytics', to: PATHS.ADMIN.SITE_ANALYTICS },
            { key: PATHS.ADMIN.ADD_ADMIN, title: 'Add Admin', to: PATHS.ADMIN.ADD_ADMIN },
        ]
        : [
            { key: PATHS.ORGANIZATION.SUBMIT_REQUEST, title: 'Submit Movie', to: PATHS.ORGANIZATION.SUBMIT_REQUEST },
            { key: PATHS.ORGANIZATION.MOVIES_ANALYTICS, title: 'My Movies & Analytics', to: PATHS.ORGANIZATION.MOVIES_ANALYTICS },
            { key: PATHS.ORGANIZATION.PROFILE(), title: 'Profile', to: PATHS.ORGANIZATION.PROFILE(user?.id) },
        ];

    return (
        <div className="navigationBar">
            <div className="navigationBar__brand">
                <Film size={20} />
            </div>
            {tabs.map(({ key, title, to }) => (
                <Link key={key} to={to} className={`nav-link ${location.pathname === to ? 'active' : ''}`}>
                    <span className="nav-item-title">{title}</span>
                </Link>
            ))}

            <ProfileAvatar className="org" />
        </div>
    );
}

export default NavBar;
