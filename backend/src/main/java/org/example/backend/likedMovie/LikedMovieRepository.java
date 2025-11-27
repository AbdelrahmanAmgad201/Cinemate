package org.example.backend.likedMovie;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LikedMovieRepository extends JpaRepository<LikedMovie, LikedMoviesID> {

}
