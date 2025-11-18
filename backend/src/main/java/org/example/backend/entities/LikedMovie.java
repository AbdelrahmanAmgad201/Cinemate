package org.example.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.User.User;
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
    private User user;

    @ManyToOne
    @MapsId("movieId")
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(name = "date")
    private LocalDateTime dateLiked;
}