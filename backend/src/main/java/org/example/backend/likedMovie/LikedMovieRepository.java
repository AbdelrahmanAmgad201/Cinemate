package org.example.backend.likedMovie;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikedMovieRepository extends JpaRepository<LikedMovie, LikedMoviesID> {
    public Page<LikedMovieView> findAllByUserIdAndIsDeletedFalse(Long userId,Pageable pageable);
}
