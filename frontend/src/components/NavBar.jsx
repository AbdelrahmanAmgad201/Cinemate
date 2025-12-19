import React, { useState, useContext, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { IoSearch } from "react-icons/io5";
import './style/navBar.css';
import ProfileAvatar from './ProfileAvatar.jsx';
import MoviesDetailsApi from '../api/movies-details-api.jsx';
import {searchForumsApi} from "../api/forum-api.jsx";

import {MAX_LENGTHS, PATHS} from "../constants/constants.jsx";
import {formatCount} from "../utils/formate.jsx";
import ForumCard from "./ForumCard.jsx";
import JoinButton from "./watch-party/JoinButton.jsx";

function NavBar() {

    const isActive = (path) => location.pathname === path;
    const [searchValue, setSearchValue] = useState('');
    const [searchType, setSearchType] = useState('movies'); // 'movies' or 'forums'
    const [movies, setMovies] = useState([]);
    const [forums, setForums] = useState([]);
    const [resultsShow, setResultsShow] = useState(false);
    const menuRef = useRef(null);
    const searchRef = useRef(null);

    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        handleSearch();
    }, [searchValue]);

    useEffect(() => {
        if (location.pathname.includes('browse') || location.pathname.includes('movie') || location.pathname.includes('genre') || location.pathname.includes('watch')) {
            setSearchType('movies');
        } else {
            setSearchType('forums');
        }
    }, [location.pathname]);

    const handleSearch = async () => {
        try{

            if (!searchValue.trim()) {
                setResultsShow(false);
                return;
            }

            if (searchType === 'forums') {
                const response = await searchForumsApi({
                    query: searchValue,
                    page: 0,
                    size: 10,
                });

                if (response.success) {
                    const rawData = response.data.forums || response.data.content || response.data || [];
                    const mapped = rawData.map(f => ({
                        id: f.id,
                        name: f.name,
                        description: f.description,
                        followerCount: formatCount(f.followerCount),
                        type: 'forum',
                    }));
                    setForums(mapped);
                    setResultsShow(true);
                } else {
                    setForums([]);
                }
            } else {
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
                        id: movie.movieID,
                        type: 'movie',
                    }));
                    setMovies(mappedMovies);
                    setResultsShow(true);
                }else {
                    console.log("Error fetching movies in else loop:", moviesResponse.message);
                    setMovies([]);
                    setResultsShow(false);
                }
            }

        }
         catch(err){
                console.log("Error fetching content:", err);
                setMovies([]);
                setResultsShow(false);
         }
     }

     
    const handleInputChange = (e) => {
        setSearchValue(e.target.value);
    };

    const handleResultMovieClick = (movieId) => {
        navigate(PATHS.MOVIE.DETAILS(movieId));
        setResultsShow(false);
        setSearchValue('');
    };

    const handleResultForumClick = (forumId) => {
        navigate(PATHS.FORUM.PAGE(forumId));
        setResultsShow(false);
        setSearchValue('');
    };

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
                Forums
            </Link>
            <Link to={PATHS.MOVIE.BROWSE} className={`navbar-button ${isActive(PATHS.MOVIE.BROWSE) ? 'active' : ''}`}>
                Movies
            </Link>
            <div className="navbar-search" ref={searchRef}>
                <input type="text" placeholder={`Search ${searchType}...`} maxLength={MAX_LENGTHS.INPUT} onChange={handleInputChange} /><IoSearch onClick={handleSearch} />
                {resultsShow && movies.length > 0 && (
                    <div className="search-results">
                            {movies.map((movie, index) => (
                                <div key={movie.id || index} className="search-result-item" onClick={() => handleResultMovieClick(movie.id)}>
                                    {movie.title}
                                </div>
                            ))}
                        </div>
                    )}

                {resultsShow && forums.length > 0 && (
                    <div className="search-results">
                        {forums.map((forum, index) => (
                            <div
                                key={forum.id || index}
                                className="search-result-item"
                                onClick={() => handleResultForumClick(forum.id)}
                            >
                                <ForumCard key={forum.id} forum={forum} />
                            </div>
                        ))}
                    </div>
                )}
            </div>  
        </div>
        <JoinButton />
        <ProfileAvatar />
        </>
    );

} export default NavBar;
