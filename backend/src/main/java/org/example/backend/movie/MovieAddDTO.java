package org.example.backend.movie;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieAddDTO {
    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 255)
    private String movieUrl;

    @Size(max = 255)
    private String thumbnailUrl;

    @Size(max = 255)
    private String trailerUrl;

    @NotNull
    @Positive
    private Integer duration;

    @NotNull
    private Genre genre;
}
