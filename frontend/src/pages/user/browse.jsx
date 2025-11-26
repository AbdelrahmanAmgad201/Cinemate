import { React, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import NavBar from '../../components/NavBar';
import Carousel from '../../components/carousel';
import MoviesList from '../../components/moviesList';
import Footer from '../../components/footer';
import p1 from '../../assets/p1.jpg';
import p2 from '../../assets/p2.jpg';
import action from '../../assets/action.jpg';
import MoviesDetailsApi from '../../api/moviesDetailsApi';

export default function Browse() {

    const [newReleases, setNewReleases] = useState([]);
    const [topRated, setTopRated] = useState([]);
    const [genres, setGenres] = useState([]);
    const [loading, setLoading] = useState(true);

    
    const navigate = useNavigate();

    useEffect(() => {
        fetchMovies();
    }, []);

    const fetchMovies = async () => {
       try{
        const newReleasesRequest = {
            name: null,
            genre: null,
            sortBy: "releaseDate",
            sortDirection: "desc",
            page: 0,
            pageSize: 6
        };

        const topRatedRequest = {
            name: null,
            genre: null,
            sortBy: "rating",
            sortDirection: "desc",
            page: 0,
            pageSize: 6
        };

        const [newReleasesResponse, topRatedResponse] = await Promise.all([
                MoviesDetailsApi(newReleasesRequest),
                MoviesDetailsApi(topRatedRequest)
            ]);
        console.log("Response from MoviesDetailsApi:", newReleasesResponse, ",", topRatedResponse);
        if (newReleasesResponse.success) {
            const mappedNewReleases = newReleasesResponse.movies.map(movie => ({
                title: movie.name,
                poster: movie.thumbnailUrl,
                duration: movie.duration,
                rating: movie.rating || "N/A"
            }));
            setNewReleases(mappedNewReleases);
        }

        if (topRatedResponse.success) {
            const mappedTopRated = topRatedResponse.movies.map(movie => ({
                title: movie.name,
                poster: movie.thumbnailUrl,
                duration: movie.duration,
                rating: movie.rating || "N/A"
            }));
            setTopRated(mappedTopRated);
        } else {
            console.log("Error fetching movies in else loop:", response.message);
        }
        setGenres([
            { title: "Mystery", poster: p1 },
            { title: "Comedy", poster: p2 },
            { title: "Animation", poster: p1 },
            { title: "Documentary", poster: p2 },
            { title: "Romance", poster: p1 },
            { title: "Thriller", poster: p2 },
            { title: "Sci-Fi", poster: p1 },
            { title: "Horror", poster: p2 },
            { title: "Drama", poster: p1 },
            { title: "Action", poster: action }
        ]);
        setLoading(false);
       }
        catch(err){
        console.log("Error fetching movies:", err);
        setLoading(false);
        }
    }

    const newReleasesTemp = [
        {
            title: "The Tuesday Murder Club",
            poster: p1,
            duration: "1h 13min",
            rating: "9.7"
        },
        {
            title: "Spirited Away",
            poster: p2,
            duration: "1h 13min",
            rating: "9.7"
        },
        {
            title: "The Tuesday Murder Club",
            poster: p1,
            duration: "1h 13min",
            rating: "9.7"
        },
        {
            title: "Spirited Away",
            poster: p2,
            duration: "1h 13min",
            rating: "9.7"
        },
        {
            title: "The Tuesday Murder Club",
            poster: p1,
            duration: "1h 13min",
            rating: "9.7"
        },
        {
            title: "Spirited Away",
            poster: p2,
            duration: "1h 13min",
            rating: "9.7"
        }
    ];

    

    return (
        <div>
            <NavBar />
            <main style={{flex: "1"}}>
                <div>
                    <Carousel />
                </div>
                <div style={{display: "flex", flexDirection: "column", gap: "60px"}}>
                    <MoviesList list={newReleases} name={"New Releases"} />
                    <MoviesList list={topRated} name={"Top Rated"} />
                    <MoviesList list={genres} name={"Genres"} onClick={(genre) => navigate(`/genre/${genre}`)} />
                </div>
            </main>
            
            <Footer />
        </div>
    );
}