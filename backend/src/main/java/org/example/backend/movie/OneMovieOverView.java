package org.example.backend.movie;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OneMovieOverView {
    private Long views;
    private Long numberOfWatchers;
    private Long likes;
    private Long numberOfRatings;
    private double averageRating;
    private Long numberOfWatchLaters;

}
