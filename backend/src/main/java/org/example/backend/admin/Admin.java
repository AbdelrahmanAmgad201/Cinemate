package org.example.backend.admin;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.movie.Movie;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admins", indexes = {
        @Index(name = "idx_admin_email", columnList = "email")
})
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @OneToMany(mappedBy = "admin")
    @Builder.Default
    private List<Movie> approvedMovies = new ArrayList<>();

}