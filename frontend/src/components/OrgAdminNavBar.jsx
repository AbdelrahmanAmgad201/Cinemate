import { useState, useContext, useEffect, useRef } from 'react';

import "../pages/admin/style/navBar.css";


function NavBar(){
 
    return(
        <div className="navigationBar">
            <Link to="/review-movies"><h1>Review Movies</h1></Link>
            <Link to="/admin-site-analytics"><h1>Site Movies and Analytics</h1></Link>
            <ProfileAvatar />
        </div>
    );

} export default NavBar;