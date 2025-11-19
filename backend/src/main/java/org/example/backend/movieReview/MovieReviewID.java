package org.example.backend.movieReview;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MovieReviewID implements Serializable {
    private Long movieId;
    private Long reviewerId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieReviewID that = (MovieReviewID) o;
        return Objects.equals(movieId, that.movieId) &&
                Objects.equals(reviewerId, that.reviewerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movieId, reviewerId);
    }
}