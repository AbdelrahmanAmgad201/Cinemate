import { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Search, Film } from 'lucide-react';
import './style/navBar.css';
import ProfileAvatar from './ProfileAvatar.jsx';
import moviesDetailsApi from '../api/movies-details-api.js';
import { searchForumsApi } from '../api/forum-api.js';

import { MAX_LENGTHS, PATHS } from '../constants/constants.jsx';
import { formatCount } from '../utils/formate.jsx';
import Avatar from './ui/Avatar.jsx';
import PartySessionHandler from './watch-party/PartySessionHandler.jsx';

const SEARCH_DEBOUNCE_MS = 300;

function NavBar() {
    const location = useLocation();
    const navigate = useNavigate();

    const isActive = (path) => location.pathname === path;

    const [searchValue, setSearchValue] = useState('');
    const [searchType, setSearchType] = useState('movies');
    const [movies, setMovies] = useState([]);
    const [forums, setForums] = useState([]);
    const [resultsShow, setResultsShow] = useState(false);
    const [searching, setSearching] = useState(false);
    const searchRef = useRef(null);

    useEffect(() => {
        if (location.pathname.includes('browse') || location.pathname.includes('movie') || location.pathname.includes('genre') || location.pathname.includes('watch')) {
            setSearchType('movies');
        } else {
            setSearchType('forums');
        }
    }, [location.pathname]);

    // Debounced — without this every keystroke fires its own API call.
    useEffect(() => {
        const timeoutId = setTimeout(() => {
            handleSearch();
        }, SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timeoutId);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [searchValue, searchType]);

    const handleSearch = async () => {
        if (!searchValue.trim()) {
            setResultsShow(false);
            return;
        }

        setSearching(true);
        try {
            if (searchType === 'forums') {
                const response = await searchForumsApi({ query: searchValue, page: 0, size: 10 });
                if (response.success) {
                    const rawData = response.data.forums || response.data.content || response.data || [];
                    setForums(rawData.map(f => ({
                        id: f.id,
                        name: f.name,
                        followerCount: formatCount(f.followerCount),
                    })));
                    setMovies([]);
                    setResultsShow(true);
                } else {
                    setForums([]);
                }
            } else {
                const moviesResponse = await moviesDetailsApi({
                    name: searchValue, genre: null, sortBy: null, sortDirection: null, page: null, pageSize: 10,
                });
                if (moviesResponse.success) {
                    setMovies(moviesResponse.movies.map(movie => ({ title: movie.name, id: movie.movieID })));
                    setForums([]);
                    setResultsShow(true);
                } else {
                    setMovies([]);
                    setResultsShow(false);
                }
            }
        } catch {
            setMovies([]);
            setForums([]);
            setResultsShow(false);
        } finally {
            setSearching(false);
        }
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
            if (searchRef.current && !searchRef.current.contains(event.target)) {
                setResultsShow(false);
            }
        };
        if (resultsShow) document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [resultsShow]);

    const hasResults = movies.length > 0 || forums.length > 0;

    return (
        <>
            <div className="navbar-spacer" />
            <header className="navbar">
                <div className="navbar__inner">
                    <Link to={PATHS.HOME} className="navbar__brand">
                        <Film size={22} aria-hidden="true" />
                        <span>Cinemate</span>
                    </Link>

                    <nav className="navbar__links" aria-label="Primary">
                        <Link to={PATHS.HOME} className={`navbar-button ${isActive(PATHS.HOME) ? 'active' : ''}`}>
                            Forums
                        </Link>
                        <Link to={PATHS.MOVIE.BROWSE} className={`navbar-button ${isActive(PATHS.MOVIE.BROWSE) ? 'active' : ''}`}>
                            Movies
                        </Link>
                    </nav>

                    <div className="navbar-search" ref={searchRef}>
                        <Search size={17} className="navbar-search__icon" aria-hidden="true" />
                        <input
                            type="text"
                            placeholder={`Search ${searchType}...`}
                            maxLength={MAX_LENGTHS.INPUT}
                            value={searchValue}
                            onChange={(e) => setSearchValue(e.target.value)}
                            onFocus={() => hasResults && setResultsShow(true)}
                            aria-label={`Search ${searchType}`}
                        />

                        {resultsShow && (
                            <div className="search-results" role="listbox">
                                {searching && <div className="search-result-empty">Searching…</div>}
                                {!searching && !hasResults && (
                                    <div className="search-result-empty">No {searchType} found for "{searchValue}"</div>
                                )}
                                {!searching && movies.map((movie, index) => (
                                    <div
                                        key={movie.id || index}
                                        className="search-result-item"
                                        role="option"
                                        aria-selected="false"
                                        onClick={() => handleResultMovieClick(movie.id)}
                                    >
                                        <Film size={15} aria-hidden="true" />
                                        {movie.title}
                                    </div>
                                ))}
                                {!searching && forums.map((forum, index) => (
                                    <div
                                        key={forum.id || index}
                                        className="search-result-item"
                                        role="option"
                                        aria-selected="false"
                                        onClick={() => handleResultForumClick(forum.id)}
                                    >
                                        <Avatar name={forum.name} size="xs" />
                                        <span className="search-result-item__title">{forum.name}</span>
                                        <span className="search-result-item__meta">{forum.followerCount}</span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </header>
            <PartySessionHandler />
            <ProfileAvatar />
        </>
    );
}

export default NavBar;
