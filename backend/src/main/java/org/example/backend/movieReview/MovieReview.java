package org.example.backend.movieReview;

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
@Table(name = "movie_review", indexes = {
        @Index(name = "idx_review_reviewer", columnList = "reviewer_id")
})
public class MovieReview {
    @EmbeddedId
    private MovieReviewID movieReviewID;

    @ManyToOne
    @MapsId("movieId")
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne
    @MapsId("reviewerId")
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        validateRating();
    }

    @PreUpdate
    protected void onUpdate() {
        validateRating();
    }

    private void validateRating() {
        if (rating == null || rating < 1 || rating > 10) {
            throw new IllegalArgumentException("Rating must be between 1 and 10");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovieReview)) return false;
        MovieReview that = (MovieReview) o;
        return movieReviewID != null && movieReviewID.equals(that.movieReviewID);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}