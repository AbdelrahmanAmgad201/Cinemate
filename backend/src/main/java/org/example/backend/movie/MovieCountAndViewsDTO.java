package org.example.backend.movie;

/**
 * Result of {@link MovieRepository#getMovieCountAndTotalViews} — a typed JPQL
 * constructor-expression projection (PERF-07) instead of an {@code Object[]} that had
 * to be unsafely cast and could throw {@code ClassCastException} at runtime if the
 * query's projection ever changed shape.
 */
public record MovieCountAndViewsDTO(long numberOfMovies, long totalViews) {
}
