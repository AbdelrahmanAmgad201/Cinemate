import { useState, useContext, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { IoSearch } from "react-icons/io5";
import { CgProfile } from "react-icons/cg";
import { AuthContext } from '../context/authContext.jsx';
import './style/navBar.css';

function NavBar() {

    const isActive = (path) => location.pathname === path;
    const [searchValue, setSearchValue] = useState('');
    const [menuShow, setMenuShow] = useState(false);
    const [resultsShow, setResultsShow] = useState(false);
    const menuRef = useRef(null);
    const searchRef = useRef(null);

    const location = useLocation();
    const navigate = useNavigate();
    const { signOut } = useContext(AuthContext);

    const [results, setResults] = useState(["Movie 1", "Movie 2", "Movie 3"]);

    const handleSearch = () => {
        console.log("Searching for:", searchValue);
        if (searchValue.trim()) {
            setResults(["Movie 1", "Movie 2", "Movie 3"]);
            setResultsShow(true);
        } else {
            setResults([]);
            setResultsShow(false);
        }
    };

    const handleInputChange = (e) => {
        setSearchValue(e.target.value);
        if (e.target.value.trim()) {
            setResults(["Movie 1", "Movie 2", "Movie 3"]);
            setResultsShow(true);
        } else {
            setResultsShow(false);
        }
    };

    const handleSignOut = async () => {
        await signOut();
        setMenuShow(false);
        navigate('/', { replace: true });
    }

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setMenuShow(false);
            }
            if (searchRef.current && !searchRef.current.contains(event.target)) {
                setResultsShow(false);
            }
        };

        if (menuShow || resultsShow) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [menuShow, resultsShow]);

    return (
        <>
        <div className="navbar-spacer"></div>
        <div className="navbar">
            <Link to="/home-page" className={`navbar-button ${isActive('/home-page') ? 'active' : ''}`}>
                Home
            </Link>
            <Link to="/browse" className={`navbar-button ${isActive('/browse') ? 'active' : ''}`}>
                Browse
            </Link>
            <div className="navbar-search" ref={searchRef}>
                <input type="text" placeholder="Search movies..." onChange={handleInputChange} onKeyPress={(e) => e.key === 'Enter' && handleSearch()} /><IoSearch onClick={handleSearch} />
                {resultsShow && results.length > 0 && (
                    <div className="search-results">
                            {results.map((result, index) => (
                                        <div key={index} className="search-result-item" onClick={() => {console.log("Clicked:", result); setResultsShow(false);}}>
                                            {result}
                                        </div>
                            ))}
                        </div>
                    )}
            </div>  
        </div>
        <div className="profile-icon" ref={menuRef} >
            <CgProfile onClick={() => setMenuShow(prev => !prev)} />
            {menuShow && (
                <div className="menu">
                    <ul>
                        <li onClick={handleSignOut}>Sign Out</li>
                    </ul>
                </div>
            )}
        </div>
        </>
    );

} export default NavBar;
