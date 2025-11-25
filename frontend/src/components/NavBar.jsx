import { useState, useContext, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { IoSearch } from "react-icons/io5";
import { CgProfile } from "react-icons/cg";
import { AuthContext } from '../context/authContext.jsx';

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
            <style>{`
                .navbar {
                    background-color: #182B33;
                    padding: 10px;
                    width: 500px;
                    height: 44px;
                    padding: 0px;
                    border-radius: 40px;
                    position: fixed;
                    top: 35px;
                    left: 50%;
                    transform: translateX(-50%);
                    padding: 0 23px;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    gap: 27px;
                }
                .navbar-button {
                    height: 100%;
                    width: 71px;
                    text-decoration: none;
                    font-size: 20px;
                    color: white;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border-radius: 20px;
                    padding: 0 11px;
                    transition: background-color 0.3s;
                }
                .navbar-button:hover, .navbar-button.active {
                    background-color: #24414D;
                }
                .navbar-search {
                    background-color: #2B3C44;
                    border-radius: 40px;
                    height: 100%;
                    width: 305px;
                    color: #AAAAAA;
                    font-size: 20px;
                    padding: 0 15px;
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                }
                .navbar-search input {
                    background-color: #2B3C44;
                    border: none;
                    border-radius: 40px;
                    height: 100%;
                    color: #AAAAAA;
                    font-size: 20px;
                    padding: 0 16px;
                }
                .navbar-search input:focus{
                    color: var(--ghost-white);
                    outline: none;
                }
                .navbar-search svg {
                    color: var(--ghost-white);
                    cursor: pointer;
                    font-size: 22px;
                }

                .search-results {
                    position: absolute;
                    top: 50px;
                    width: 305px;
                    background-color: #182B33;
                    color: var(--ghost-white);
                    border-radius: 5px;
                    max-height: 200px;
                    overflow-y: auto;
                    z-index: 1;

                }

                .search-result-item {
                    padding: 10px;
                    border-bottom: 1px solid #24414D;
                }
                .search-result-item:hover {
                    background-color: #24414D;
                    cursor: pointer;
                }

                .profile-icon {
                    position: fixed;
                    right: 50px;
                    top: 35px;
                }

                .profile-icon svg{
                    height: 40px;
                    width: 40px;
                    cursor: pointer;
                }
                .menu {
                    position: absolute;
                    right: 0px;
                    top: 50px;
                    background-color: #182B33;
                    border-radius: 5px;
                    color: var(--ghost-white);
                    cursor: pointer;
                    min-width: 120px;
                }

                .menu ul {
                    list-style: none;
                    margin: 0;
                    padding: 0;
                }   
                .menu li {
                    padding: 10px;
                    height: 100%;
                    border-bottom: 1px solid #24414D;
                }
                .menu li:hover {
                    background-color: #24414D;
                }
            `}
            </style>
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
