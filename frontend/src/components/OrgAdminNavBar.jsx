import { useState, useContext, useEffect, useRef } from 'react';
import {Link} from 'react-router-dom';
import ProfileAvatar from './profileAvatar';
import "../pages/admin/style/navBar.css";


function NavBar(){
 
    return(
        <div className="navigationBar">
            <Link to="/review-movies"><h1>Review Movies</h1></Link>
            <Link to="/admin-site-analytics"><h1>Site Movies and Analytics</h1></Link>
            <ProfileAvatar className='org'/>
        </div>
    );

} export default NavBar;