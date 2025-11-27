import {Link} from "react-router-dom";

import "./style/navBar.css"
import ProfileAvatar from "../../components/profileAvatar.jsx";
import {useContext} from "react";
import {AuthContext} from "../../context/authContext.jsx";

export default function ReviewMovies() {

    const {signOut} = useContext(AuthContext);
    const avatarMenuItems = [
        // { label: "Profile", onClick: () => console.log("Profile clicked") },
        // { label: "Settings", onClick: () => console.log("Settings clicked") },
        { label: "Sign Out", onClick: signOut },
    ];

    return (
        <div>
            <div className="navigationBar">
                <Link to="/review-movies"><h1>Review Movies</h1></Link>
                <Link to="/admin-site-analytics"><h1>Site Movies and Analytics</h1></Link>
                <ProfileAvatar menuItems={avatarMenuItems} />
            </div>
        </div>
    )

}