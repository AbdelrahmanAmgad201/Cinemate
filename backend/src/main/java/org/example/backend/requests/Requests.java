package org.example.backend.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.example.backend.admin.Admin;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    private Admin admin;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
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
    @JsonProperty("organization")
    public String getOrganizationName() {
        return organization != null ? organization.getName() : null;
    }
}
