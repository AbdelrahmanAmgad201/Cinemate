import { useState, useRef, useContext, useEffect } from "react";
import { CgProfile } from "react-icons/cg";
import { AuthContext } from '../context/authContext.jsx';
import "./style/profileAvatar.css";

export default function ProfileAvatar({ className="" }) {
    const [menuShow, setMenuShow] = useState(false);
    const { signOut, user } = useContext(AuthContext);
    const menuRef = useRef(null);

    const menuItems = (() => {
        if(user?.role === "USER"){
            return [{ label: "Sign Out", onClick: signOut }];
        }
        else if(user?.role === "ORGANIZATION"){
            return [{ label: "Sign Out", onClick: signOut }];
        }
        return [{ label: "Sign Out", onClick: signOut }];
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
