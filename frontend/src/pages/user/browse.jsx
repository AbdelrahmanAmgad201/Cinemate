import React from 'react';
import NavBar from '../../components/navBar';
import Carousel from '../../components/carousel';
import MoviesList from '../../components/moviesList';
import p1 from '../../assets/p1.jpg';
import p2 from '../../assets/p2.jpg';

export default function Browse() {

    const newReleases = [
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

    const genres = [
        {
            title: "Mystery",
            poster: p1
        },
        {
            title: "Comedy",
            poster: p2
        },
        {
            title: "Animation",
            poster: p1
        },
        {
            title: "Documentary",
            poster: p2
        },
        {
            title: "Romance",
            poster: p1
        },
        {
            title: "Thriller",
            poster: p2
        },
        {
            title: "Sci-Fi",
            poster: p1
        },
        {
            title: "Horror",
            poster: p2
        },
        {
            title: "Drama",
            poster: p1
        },
        {
            title: "Action",
            poster: p2
        }
    ];

    return (
        <div>
            <NavBar />
            <div>
                <Carousel />
            </div>
            <div style={{display: "flex", flexDirection: "column", gap: "60px"}}>
                <MoviesList list={newReleases} name={"New Releases"}/>
                <MoviesList list={newReleases} name={"Top Rated"}/>
                <MoviesList list={genres} name={"Genres"}/>
            </div>
            <footer style={{height: "247px", backgroundColor: "#1A3039", marginTop: "66px"}}>

            </footer>
        </div>
    );
}