package org.example.backend.requests;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.movie.Movie;
import org.example.backend.organization.Organization;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "requests_mails")
public class Requests {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "movie_name")
    private String movieName;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "state_update_at",nullable = true)
    private LocalDateTime stateUpdatedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private State state;
    @Column (name = "admin_id")
    private Long adminId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = true)
    private Movie movie;
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
