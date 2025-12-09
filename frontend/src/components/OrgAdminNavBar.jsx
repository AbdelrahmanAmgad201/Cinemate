import { useState, useContext, useEffect, useRef } from 'react';
import {Link} from 'react-router-dom';
import ProfileAvatar from './profileAvatar';
import "../pages/admin/style/navBar.css";
import PropTypes from "prop-types";


function NavBar({tabs}){
 
    return(
        <div className="navigationBar">
            {tabs.map(({key, title, to}) => (
                <Link key={key} to={to}><h1>{title}</h1></Link>
            ))}
            <ProfileAvatar className='org'/>
        </div>
    );

} export default NavBar;

NavBar.propTypes = {
    tabs: PropTypes.arrayOf(
        PropTypes.shape({
            key: PropTypes.string,
            title: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
            to: PropTypes.string.isRequired,
        })
    ),
};