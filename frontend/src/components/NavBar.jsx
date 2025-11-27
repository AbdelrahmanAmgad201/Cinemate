import { useState, useContext, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { IoSearch } from "react-icons/io5";
import { CgProfile } from "react-icons/cg";
import { AuthContext } from '../context/authContext.jsx';
import MoviesDetailsApi from '../api/moviesDetailsApi.jsx';
import './style/navBar.css';

function NavBar() {

    const isActive = (path) => location.pathname === path;
    const [searchValue, setSearchValue] = useState('');
    const [movies, setMovies] = useState([]);
    const [menuShow, setMenuShow] = useState(false);
    const [resultsShow, setResultsShow] = useState(false);
    const menuRef = useRef(null);
    const searchRef = useRef(null);

    const location = useLocation();
    const navigate = useNavigate();
    const { signOut } = useContext(AuthContext);

    const [results, setResults] = useState(["Movie 1", "Movie 2", "Movie 3"]);

    useEffect(() => {
        handleSearch();
    }, [searchValue]);
    
    const handleSearch = async () => {
        try{

            if (!searchValue.trim()) {
                setResultsShow(false);
                return;
            }

            const moviesRequest = {
                name: searchValue,
                genre: null,
                sortBy: null,
                sortDirection: null,
                page: null,
                pageSize: 10
            };

            const moviesResponse = await MoviesDetailsApi(moviesRequest);

            console.log("Response from MoviesDetailsApi:", moviesResponse);
            if (moviesResponse.success) {
                const mappedMovies = moviesResponse.movies.map(movie => ({
                    title: movie.name,
                    id: movie.movieID
                }));
                setMovies(mappedMovies);
                setResultsShow(true);
            }else {
                console.log("Error fetching movies in else loop:", response.message);
                setMovies([]);
                setResultsShow(false);
            }
        }
         catch(err){
                console.log("Error fetching movies:", err);
                setMovies([]);
                setResultsShow(false);
         }
     }

     
    const handleInputChange = (e) => {
        setSearchValue(e.target.value);
    };

    const handleResultClick = (movieId) => {
        navigate(`/movie/${movieId}`);
        setResultsShow(false);
        setSearchValue('');
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
                <input type="text" placeholder="Search movies..." onChange={handleInputChange} /><IoSearch onClick={handleSearch} />
                {resultsShow && movies.length > 0 && (
                    <div className="search-results">
                            {movies.map((movie, index) => (
                                        <div key={movie.id || index} className="search-result-item" onClick={() => handleResultClick(movie.id)}>
                                            {movie.title}
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
