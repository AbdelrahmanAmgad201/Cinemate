package org.example.backend.movie;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieAddDTO {
    private String name;
    private String description;
    private String movieUrl;
    private String thumbnailUrl;
    private String trailerUrl;
    private Integer duration;
    private Genre genre;
}
