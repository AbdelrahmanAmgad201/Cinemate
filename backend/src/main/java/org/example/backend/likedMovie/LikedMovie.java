package org.example.backend.likedMovie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.example.backend.movie.Movie;
import org.example.backend.user.User;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "liked_movies", indexes = {
        @Index(name = "idx_liked_movie_user_id", columnList = "user_id")
})
public class LikedMovie {
    @EmbeddedId
    private LikedMoviesID likedMoviesID;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne
    @MapsId("movieId")
    @JoinColumn(name = "movie_id")
    @JsonIgnore
    private Movie movie;

    @Column(name = "date")
    private LocalDateTime dateLiked;

    @PrePersist
    protected void onCreate() {
        if (dateLiked == null) {
            dateLiked = LocalDateTime.now();
        }
    }
}