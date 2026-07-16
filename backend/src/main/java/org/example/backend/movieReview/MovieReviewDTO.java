package org.example.backend.movieReview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieReviewDTO {
    @NotNull
    private Long movieId;

    @Size(max = 2000)
    private String comment;

    // Matches the range MovieReview.java itself enforces.
    @NotNull
    @Min(1)
    @Max(10)
    private Integer rating;
}
