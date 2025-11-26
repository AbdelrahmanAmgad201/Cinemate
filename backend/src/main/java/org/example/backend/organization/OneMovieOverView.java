package org.example.backend.organization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.backend.movie.Genre;

@Getter
@Setter
@AllArgsConstructor
public class OneMovieOverView {
    Long views;
    Long numberOfWatchers;
    Long likes;
    Long numberOfRatings;
    double averageRating;
    Long numberOfWatchLaters;

}
