package org.example.backend.movie;

import org.example.backend.organization.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    List<Movie> findByAdminIsNull();
    @Query("""
   SELECT 
     COUNT(m), 
     (SELECT COUNT(wh) 
      FROM WatchHistory wh 
      JOIN Movie m2 ON m2.id = wh.movieId
      WHERE m2.organization.id = :orgId
      AND (wh.isDeleted = false OR wh.isDeleted IS NULL))
   FROM Movie m
   WHERE m.organization.id = :orgId
   """)
    Object getMovieCountAndTotalViews(@Param("orgId") Long orgId);

    @Query("""
   SELECT m.genre, COUNT(wh) as views
   FROM WatchHistory wh
   JOIN Movie m ON m.id = wh.movieId
   WHERE m.organization.id = :orgId
   AND (wh.isDeleted = false OR wh.isDeleted IS NULL)
   GROUP BY m.genre
   ORDER BY views DESC
   """)
    List<Object[]> getGenresOrderedByViews(@Param("orgId") Long orgId);

    Page<Movie> findAllByAdminIsNotNull(Specification<Movie> spec, Pageable pageable);

    List<Movie> findByAdminIsNotNullAndOrganization_Id(Long orgId);

    @Query("""
        SELECT new org.example.backend.movie.OneMovieOverView(
            (SELECT COUNT(wh) FROM WatchHistory wh WHERE wh.movieId = :movieId AND (wh.isDeleted = false OR wh.isDeleted IS NULL)),
            (SELECT COUNT(DISTINCT wh.user.id) FROM WatchHistory wh WHERE wh.movieId = :movieId AND (wh.isDeleted = false OR wh.isDeleted IS NULL)),
            (SELECT COUNT(mr) FROM MovieReview mr WHERE mr.movie.movieID = :movieId),
            (SELECT COUNT(mr) FROM MovieReview mr WHERE mr.movie.movieID = :movieId),
            (SELECT COALESCE(AVG(mr.rating),0) FROM MovieReview mr WHERE mr.movie.movieID = :movieId),
            (SELECT COUNT(wl) FROM WatchLater wl WHERE wl.movie.movieID = :movieId)
        )
        """)
    OneMovieOverView getMovieOverview(@Param("movieId") Long movieId);

    @Query("""
       SELECT lm.movie.movieID
       FROM LikedMovie lm
       GROUP BY lm.movie.movieID
       ORDER BY COUNT(lm) DESC
       """)
    List<Long> getMostLikedMovie(Pageable pageable);

    @Query("""
       SELECT mr.movie.movieID
       FROM MovieReview mr
       GROUP BY mr.movie.movieID
       ORDER BY COUNT(mr) DESC
       """)
    List<Long> getMostRatedMovie(Pageable pageable);


    @Query("""
    SELECT m.organization 
    FROM WatchHistory wh 
    JOIN Movie m ON m.movieID = wh.movieId 
    WHERE wh.isDeleted = false OR wh.isDeleted IS NULL
    GROUP BY m.organization 
    ORDER BY COUNT(wh) DESC
    """)
    List<Organization> getMostPopularOrganization(Pageable pageable);


    Long countByAdminIsNotNull();
}