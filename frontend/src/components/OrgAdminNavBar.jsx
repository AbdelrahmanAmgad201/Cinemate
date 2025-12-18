import { useState, useContext, useEffect, useRef } from 'react';
import {Link} from 'react-router-dom';
import ProfileAvatar from './ProfileAvatar';
import { AuthContext } from '../context/AuthContext.jsx';
import "../pages/admin/style/NavBar.css";

import {ROLES, PATHS} from "../constants/constants.jsx";


function NavBar(){
    
    const { user } = useContext(AuthContext);

    const tabs = (() => {
        if(user?.role === ROLES.ADMIN){
            return [
                {key: PATHS.ADMIN.REVIEW_REQUESTS, title: "Review Movie Requests", to: PATHS.ADMIN.REVIEW_REQUESTS},
                {key: PATHS.ADMIN.SITE_ANALYTICS, title: "Site Movies and Analytics", to: PATHS.ADMIN.SITE_ANALYTICS},
                {key: PATHS.ADMIN.ADD_ADMIN, title: "Add New Admin", to: PATHS.ADMIN.ADD_ADMIN},
            ];
        }
        return [
            {key: PATHS.ORGANIZATION.SUBMIT_REQUEST, title: "Submit Movie", to: PATHS.ORGANIZATION.SUBMIT_REQUEST},
            {key: PATHS.ORGANIZATION.MOVIES_ANALYTICS, title: "My Movies Analytics", to: PATHS.ORGANIZATION.MOVIES_ANALYTICS},
        ];
    })();

    return(
        <div className="navigationBar">
            {tabs.map(({key, title, to}) => (
                <Link key={key} to={to}><h1>{title}</h1></Link>
            ))}
            <ProfileAvatar className='org'/>
        </div>
    );

} export default NavBar;
