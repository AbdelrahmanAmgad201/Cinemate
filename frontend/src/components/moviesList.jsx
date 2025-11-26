import React from 'react';
import './style/moviesList.css';
import { MdOutlineStar } from "react-icons/md";

export default function MoviesList({ list, name, onClick }) {
    return (
        <div className="movie-list">
            <h2 className="list-title">{name}</h2>
            <div className="list-container">
                {list.map((movie, index) => (
                    <div key={index} className="movie-item" onClick={() => onClick && onClick(movie.title)}>
                        <img src={movie.poster} alt={movie.title} className="movie-poster" />
                        <h3 className="movie-title">{movie.title}</h3>
                        {movie.duration && movie.rating && (
                            <div className="movie-details">
                                <div style={{marginBottom: "10px"}} className="duration">{movie.duration}</div>
                                <div className="rating" style={{display: "flex",alignItems: "center", gap: "6px"}}><MdOutlineStar style={{color: "#ffc107"}}/> {movie.rating}/10</div>
                            </div>
                        )}
                        
                    </div>
                ))}
            </div>
        </div>
        
    );
};