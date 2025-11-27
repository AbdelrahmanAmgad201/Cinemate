package org.example.backend.movieReview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieReviewDTO {
    private Long movieId;
    private String comment;
    private Integer rating;
}
