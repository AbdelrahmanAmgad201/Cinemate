import React from 'react';
import './style/moviesList.css';
import { MdOutlineStar } from "react-icons/md";
import { MdNavigateNext } from "react-icons/md";
import { MdNavigateBefore } from "react-icons/md";


function MoviesList({ list, name, page, setPage, onClick }) {

    return (
        <div className="movie-list">
            <h2 className="list-title">{name}</h2>
            <div className="list-container">
                {list.length === 0 ? (
                    <div style={{marginTop: "100px"}}>
                        <p style={{fontSize: "30px"}}>Nothing Here...</p>
                    </div>
                )
                :
                list.map((movie, index) => (
                    <div key={movie.id ?? movie.title ?? index} className="movie-item" onClick={() => onClick && onClick(movie.id != null ? movie.id : movie.title)}>
                        <img src={movie.poster} alt={movie.title} className="movie-poster" width="174" height="258" loading="lazy" />
                        <h3 className="movie-title">{movie.title}</h3>
                        {movie.duration && movie.rating && (
                            <div className="movie-details">
                                <div style={{marginBottom: "10px"}} className="duration">{Math.floor(movie.duration/60)}h {movie.duration%60}min</div>
                                <div className="rating" style={{display: "flex",alignItems: "center", gap: "6px"}}><MdOutlineStar style={{color: "#ffc107"}}/> {movie.rating}/10</div>
                            </div>
                        )}

                    {list.length === 0 && (
                        <div>
                            <p>Nothing Here</p>
                        </div>
                        )}
                        
                    </div>
                ))}
                
                
            </div>
            {page >= 0 &&(
                <div className="paging">
                    <MdNavigateBefore className={(page - 1) > -1 ? "paging-icon" : "inactive"} onClick={() => (page - 1) > -1 && setPage(page - 1)} />
                    {page + 1}
                    <MdNavigateNext className={(list.length === 6 || list.length === 20) ? "paging-icon" : "inactive"} onClick={() => (list.length === 6 || list.length === 20) && setPage(page + 1)} />
                </div>
            )}
        </div>
        
    );
};

export default React.memo(MoviesList);