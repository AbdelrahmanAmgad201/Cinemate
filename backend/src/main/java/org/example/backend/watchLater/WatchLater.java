package org.example.backend.watchLater;

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
@Table(name = "watch_later", indexes = {
        @Index(name = "idx_watch_later_user_id", columnList = "user_id")
})
public class WatchLater {
    @EmbeddedId
    private WatchLaterID watchLaterID;

    @ManyToOne
    @MapsId("id")
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne
    @MapsId("movieId")
    @JoinColumn(name = "movie_id")
    @JsonIgnore
    private Movie movie;

    @Column(name = "movie_name", nullable = false)
    private String movieName;

    @Column(name = "date")
    private LocalDateTime dateAdded;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        dateAdded = LocalDateTime.now();
    }
}