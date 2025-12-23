import { useState, useRef, useContext, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { CgProfile } from "react-icons/cg";
import { AuthContext } from '../context/AuthContext.jsx';
import "./style/profileAvatar.css";

import { PATHS, ROLES } from "../constants/constants.jsx";

export default function ProfileAvatar({ className="" }) {
    const [menuShow, setMenuShow] = useState(false);
    const { signOut, user } = useContext(AuthContext);
    const menuRef = useRef(null);
    const navigate = useNavigate();

    const handleViewProfile = () => {
        setMenuShow(false);
        if (user?.id) {
            navigate(PATHS.USER.PROFILE(user.id));
        }
    }

    const handleSignOut = () => {
        setMenuShow(false);
        signOut();
    }

    const menuItems = (() => {
        if (!user) return [];

        if (user?.role === ROLES.USER) {
            return [
                { label: "View Profile", onClick: handleViewProfile },
                { label: "Sign Out", onClick: handleSignOut },
            ];
        }

        return [{ label: "Sign Out", onClick: handleSignOut }];
    })();

    // Close dropdown if clicked outside
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (menuRef.current && !menuRef.current.contains(e.target)) {
                setMenuShow(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className={`profile-avatar ${className}`} ref={menuRef}>
            <CgProfile size="32" onClick={() => setMenuShow(prev => !prev)} />

            {menuShow && (
                <div className="profile-menu">
                    <ul>
                        {menuItems.length > 0
                            ? menuItems.map((item, index) => (
                                <li key={index} onClick={item.onClick}>{item.label}</li>
                            ))
                            : <li onClick={signOut}>Sign Out</li>
                        }
                    </ul>
                </div>
            )}
        </div>
    );
}
