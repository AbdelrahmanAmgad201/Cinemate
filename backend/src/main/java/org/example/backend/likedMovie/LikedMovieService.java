package org.example.backend.likedMovie;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.movieReview.MovieReviewID;
import org.example.backend.movieReview.MovieReviewRepository;
import org.example.backend.user.PrivateProfileException;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikedMovieService {
    private final LikedMovieRepository likedMovieRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Transactional
    public LikedMovie likeMovie(Long userId, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LikedMoviesID id = new LikedMoviesID(userId, movieId);

        Optional<LikedMovie> existingLike = likedMovieRepository.findById(id);
        if (existingLike.isPresent()) {
            if(existingLike.get().getIsDeleted()) {
                existingLike.get().setIsDeleted(false);
                likedMovieRepository.save(existingLike.get());
            }
            return existingLike.get();
        }

        LikedMovie likedMovie = LikedMovie.builder()
                .likedMoviesID(id)
                .user(user)
                .movieName(movie.getName())
                .build();

        return likedMovieRepository.save(likedMovie);
    }

    @Transactional
    public void unlikeMovie(Long userId, Long movieId) {
        LikedMoviesID id = new LikedMoviesID(userId, movieId);
        Optional<LikedMovie> existingLike = likedMovieRepository.findById(id);
        if (existingLike.isPresent() && !existingLike.get().getIsDeleted()) {
            existingLike.get().setIsDeleted(true);
            likedMovieRepository.save(existingLike.get());
        }
    }

    @Transactional
    public Page<LikedMovieView> getMyLikedMovies(Long userId, Pageable pageable) {
        return getLikedMovies(userId, pageable);
    }

    @Transactional
    public Page<LikedMovieView> getOtherUserLikedMovies(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if(user.getIsPublic()){
            return getLikedMovies(userId, pageable);
        }
        throw new PrivateProfileException("this profile is private");
    }

    @Transactional
    public Boolean isLiked(Long userId,Long movieId){
        LikedMoviesID id = new LikedMoviesID(userId, movieId);
        Optional<LikedMovie> existingLike = likedMovieRepository.findById(id);
        return existingLike.isPresent() && !existingLike.get().getIsDeleted();
    }

    private Page<LikedMovieView> getLikedMovies(Long userId,Pageable pageable) {
        return likedMovieRepository.findAllByUserIdAndIsDeletedFalse(userId,pageable);
    }

}
