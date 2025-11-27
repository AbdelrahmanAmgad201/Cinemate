import { useState, useRef, useEffect } from "react";
import { CgProfile } from "react-icons/cg";
import "./style/profileAvatar.css";

export default function ProfileAvatar({ onSignOut, avatarSize = 32, menuItems = [] }) {
    const [menuShow, setMenuShow] = useState(false);
    const menuRef = useRef(null);

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
        <div className="profile-avatar" ref={menuRef}>
            <CgProfile size={avatarSize} onClick={() => setMenuShow(prev => !prev)} />

            {menuShow && (
                <div className="profile-menu">
                    <ul>
                        {menuItems.length > 0
                            ? menuItems.map((item, index) => (
                                <li key={index} onClick={item.onClick}>{item.label}</li>
                            ))
                            : <li onClick={onSignOut}>Sign Out</li>
                        }
                    </ul>
                </div>
            )}
        </div>
    );
}
