package org.example.backend.movie;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieRequestDTO {
    String name;
    Genre genre;
    String sortBy; // releaseDate - rating
    String sortDirection; // asc / desc

    @Min(0)
    int page;

    // Caps the page size (API-NEW-01) instead of trusting the client not to ask for
    // an unbounded result set.
    @Min(1)
    @Max(100)
    int pageSize;
}
