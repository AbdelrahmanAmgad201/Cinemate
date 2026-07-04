package org.example.backend.movieReview;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Review list/detail view (CQ-NEW-03, CQ-NEW-02) — returned instead of the
 * {@link MovieReview} entity, which can't be serialized directly: {@code movie} and
 * {@code reviewer} each have both a {@code @JsonIgnore}d field and a same-named
 * {@code @JsonProperty} getter.
 */
@Data
@Builder
public class MovieReviewDetailsDTO {
    private MovieReviewID movieReviewID;
    private String movie;
    private UserSummaryDTO reviewer;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static MovieReviewDetailsDTO from(MovieReview review) {
        return MovieReviewDetailsDTO.builder()
                .movieReviewID(review.getMovieReviewID())
                .movie(review.getMovieName())
                .reviewer(review.getReviewerSummary())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
