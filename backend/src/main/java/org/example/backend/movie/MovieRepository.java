package org.example.backend.movie;

import org.example.backend.organization.OneMovieOverView;
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
          WHERE wh.movie.organization.id = :orgId)
       FROM Movie m
       WHERE m.organization.id = :orgId
       """)
    Object getMovieCountAndTotalViews(@Param("orgId") Long orgId);

    @Query("""
       SELECT m.genre, COUNT(wh) as views
       FROM WatchHistory wh
       JOIN wh.movie m
       WHERE m.organization.id = :orgId
       GROUP BY m.genre
       ORDER BY views DESC
       """)
    List<Object[]> getGenresOrderedByViews(@Param("orgId") Long orgId);

    Page<Movie> findAllByAdminIsNotNull(Specification<Movie> spec, Pageable pageable);

    List<Movie> findByAdminIsNotNullAndOrganization_Id(Long orgId);

    @Query("""
            SELECT new org.example.backend.organization.OneMovieOverView(
                (SELECT COUNT(wh) FROM WatchHistory wh WHERE wh.movie.movieID = :movieId),
                (SELECT COUNT(DISTINCT wh.user.id) FROM WatchHistory wh WHERE wh.movie.movieID = :movieId),
                (SELECT COUNT(mr) FROM MovieReview mr WHERE mr.movie.movieID = :movieId),
                (SELECT COUNT(mr) FROM MovieReview mr WHERE mr.movie.movieID = :movieId),
                (SELECT COALESCE(AVG(mr.rating),0) FROM MovieReview mr WHERE mr.movie.movieID = :movieId),
                (SELECT COUNT(wl) FROM WatchLater wl WHERE wl.movie.movieID = :movieId)
            )
            """)
    OneMovieOverView getMovieOverview(@Param("movieId") Long movieId);


}