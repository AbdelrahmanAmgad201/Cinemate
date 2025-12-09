import { useState, useContext, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { IoSearch } from "react-icons/io5";
import './style/navBar.css';
import ProfileAvatar from './profileAvatar.jsx';
import MoviesDetailsApi from '../api/movies-details-api.jsx';

import {MAX_LENGTHS, PATHS} from "../constants/constants.jsx";


function NavBar() {

    const isActive = (path) => location.pathname === path;
    const [searchValue, setSearchValue] = useState('');
    const [movies, setMovies] = useState([]);
    const [resultsShow, setResultsShow] = useState(false);
    const menuRef = useRef(null);
    const searchRef = useRef(null);

    const location = useLocation();
    const navigate = useNavigate();

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
        navigate(PATHS.MOVIE.DETAILS(movieId));
        setResultsShow(false);
        setSearchValue('');
    };

    const handleSignOut = async () => {
        await signOut();
        setMenuShow(false);
        navigate(PATHS.ROOT, { replace: true });
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

        if (resultsShow) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [resultsShow]);

    return (
        <>
        <div className="navbar-spacer"></div>
        <div className="navbar">
            <Link to={PATHS.HOME} className={`navbar-button ${isActive(PATHS.HOME) ? 'active' : ''}`}>
                Home
            </Link>
            <Link to={PATHS.MOVIE.BROWSE} className={`navbar-button ${isActive(PATHS.MOVIE.BROWSE) ? 'active' : ''}`}>
                Browse
            </Link>
            <div className="navbar-search" ref={searchRef}>
                <input type="text" placeholder="Search movies..." maxLength={MAX_LENGTHS.INPUT} onChange={handleInputChange} /><IoSearch onClick={handleSearch} />
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
        <ProfileAvatar />
        </>
    );

} export default NavBar;
