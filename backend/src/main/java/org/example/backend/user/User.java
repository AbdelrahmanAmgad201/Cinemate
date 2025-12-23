package org.example.backend.user;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.userfollowing.Follows;
import org.example.backend.likedMovie.LikedMovie;
import org.example.backend.movieReview.MovieReview;
import org.example.backend.security.Authenticatable;
import org.example.backend.watchHistory.WatchHistory;
import org.example.backend.watchLater.WatchLater;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email")
})
public class User implements Authenticatable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password"/*, nullable = false */)  // google oauth users will have this field null (no password)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "about", columnDefinition = "TEXT")
    private String about;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "provider")
    @Builder.Default
    private String provider = "local";

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "number_of_followers")
    private Integer numberOfFollowers;

    @Column(name = "number_of_following")
    private Integer numberOfFollowing;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if(provider == null){
            provider = "local";
        }
        if(numberOfFollowers == null){
            numberOfFollowers = 0;
        }
        if(numberOfFollowing == null){
            numberOfFollowing = 0;
        }
    }

    public String getRole(){
        return "ROLE_USER";
    }

    @Override
    public String getName() {
        if (firstName != null){
            return firstName;
        }
        return email;
    }

    // Helper method to check if the user is an OAuth user (can change password or not)
    public boolean isOAuthUser() {
        return !"local".equals(provider);
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WatchHistory> watchHistory = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WatchLater> watchLater = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LikedMovie> likedMovies = new ArrayList<>();

    @OneToMany(mappedBy = "followingUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Follows> following = new ArrayList<>();

    @OneToMany(mappedBy = "followedUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Follows> followers = new ArrayList<>();

    @OneToMany(mappedBy = "reviewer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MovieReview> reviews = new ArrayList<>();
}