import { React, useState, useEffect} from 'react';
import { useParams } from 'react-router-dom';
import MoviesList from '../../components/moviesList';
import p1 from '../../assets/p1.jpg';
import p2 from '../../assets/p2.jpg';
import NavBar from '../../components/NavBar';
import Footer from '../../components/footer';

const Genre = ({ listGenre }) => {

    const [loading, setLoading] = useState(true);
    const [movies, setMovies] = useState([]);;
    const { title } = useParams();

    console.log("name:", title);

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

    useEffect(() => {
        fetchMovies();
    }, [title]);

    const fetchMovies = async () => {
       try{
        const moviesRequest = {
            name: null,
            genre: title.toUpperCase(),
            sortBy: null,
            sortDirection: null,
            page: 0,
            pageSize: 20
        };

        const moviesResponse = await MoviesDetailsApi(moviesRequest);

        console.log("Response from MoviesDetailsApi:", moviesResponse);
        if (moviesResponse.success) {
            const mappedMovies = moviesResponse.movies.map(movie => ({
                title: movie.name,
                poster: movie.thumbnailUrl,
                duration: movie.duration,
                rating: movie.rating || "N/A"
            }));
            setMovies(mappedMovies);
        }else {
            console.log("Error fetching movies in else loop:", response.message);
        }
        setLoading(false);
       }
        catch(err){
            console.log("Error fetching movies:", err);
            setLoading(false);
        }
    }

    return(
        <div style={{ 
            display: 'flex', 
            flexDirection: 'column', 
            minHeight: '100vh'}}>
                <main style={{ flex: '1' }}>
                    <div style={{display: "flex", flexDirection: "column", gap: "60px"}}>
                        <NavBar />
                        <MoviesList list={newReleasesTemp} name={title} />
                    </div>
                </main>
            <Footer />
        </div>
        
    );
};

export default Genre;