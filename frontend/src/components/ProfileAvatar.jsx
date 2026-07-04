import { useState, useRef, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, LogOut } from 'lucide-react';
import { AuthContext } from '../context/AuthContext.jsx';
import Avatar from './ui/Avatar.jsx';
import './style/profileAvatar.css';

import { PATHS, ROLES } from '../constants/constants.jsx';

export default function ProfileAvatar({ className = '' }) {
    const [menuShow, setMenuShow] = useState(false);
    const { signOut, user } = useContext(AuthContext);
    const menuRef = useRef(null);
    const navigate = useNavigate();

    const handleViewProfile = () => {
        setMenuShow(false);
        if (user?.id) navigate(PATHS.USER.PROFILE(user.id));
    };

    const handleViewAdminProfile = () => {
        setMenuShow(false);
        if (user?.id) navigate(PATHS.ADMIN.PROFILE(user.id));
    };

    const handleSignOut = () => {
        setMenuShow(false);
        signOut();
    };

    const menuItems = (() => {
        if (!user) return [];
        if (user.role === ROLES.USER) {
            return [
                { label: 'View Profile', icon: <User size={15} />, onClick: handleViewProfile },
                { label: 'Sign Out', icon: <LogOut size={15} />, onClick: handleSignOut },
            ];
        }
        if (user.role === ROLES.ADMIN) {
            return [
                { label: 'View Profile', icon: <User size={15} />, onClick: handleViewAdminProfile },
                { label: 'Sign Out', icon: <LogOut size={15} />, onClick: handleSignOut },
            ];
        }
        return [{ label: 'Sign Out', icon: <LogOut size={15} />, onClick: handleSignOut }];
    })();

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (menuRef.current && !menuRef.current.contains(e.target)) setMenuShow(false);
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div className={`profile-avatar ${className}`} ref={menuRef}>
            <button
                type="button"
                className="profile-avatar__trigger"
                onClick={() => setMenuShow((prev) => !prev)}
                aria-haspopup="menu"
                aria-expanded={menuShow}
                aria-label="Account menu"
            >
                <Avatar name={user?.email} size="sm" />
            </button>

            {menuShow && (
                <div className="profile-menu" role="menu">
                    {menuItems.map((item, index) => (
                        <button type="button" key={index} role="menuitem" onClick={item.onClick}>
                            {item.icon}
                            {item.label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
