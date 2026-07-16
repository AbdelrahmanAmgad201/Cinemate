package org.example.backend.movie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.example.backend.admin.Admin;
import org.example.backend.movieReview.UserSummaryDTO;
import org.example.backend.organization.Organization;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "movies", indexes = {
        @Index(name = "idx_movie_genre", columnList = "genre"),
        @Index(name = "idx_movie_rating", columnList = "average_rating"),
        @Index(name = "idx_movie_admin", columnList = "admin_id")
})
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieID;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "movie_url")
    private String movieUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "trailer_url")
    private String trailerUrl;

    @Column(name = "duration")
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", length = 20)
    private Genre genre;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "rating_sum")
    private Long ratingSum;

    @Column(name = "rating_count")
    private Integer ratingCount;

    // Generated column: rating_sum / rating_count. @Generated re-reads it after the
    // app updates rating_sum/rating_count, so movie.getAverageRating() stays correct.
    @org.hibernate.annotations.Generated(event = {
            org.hibernate.generator.EventType.INSERT,
            org.hibernate.generator.EventType.UPDATE})
    @Column(name = "average_rating", insertable = false, updatable = false)
    private Double averageRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({"id", "email", "password", "about", "createdAt", "releasedMovies"})
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = true)
    @JsonIgnore
    private Admin admin;

    @JsonProperty("organization")
    public String getOrganizationName() {
        return organization != null ? organization.getName() : null;
    }
    @PrePersist
    protected void onCreate() {
        if (ratingSum == null) {
            ratingSum = 0L;
        }
        if (ratingCount == null) {
            ratingCount = 0;
        }
        // average_rating is a generated column — computed by the database.
    }
    @PreUpdate
    protected void onUpdate() {
        if (releaseDate == null && admin !=null) {
            releaseDate = LocalDate.now();
        }
    }
}