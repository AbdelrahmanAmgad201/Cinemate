package org.example.backend.organization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.backend.movie.Genre;

@Getter
@Setter
@AllArgsConstructor
public class MoviesOverview {
    int numberOfMovies;
    int totalViewsAcrossAllMovies;
    Genre mostPopularGenre;
}
