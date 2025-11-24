package org.example.backend.movie;

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
    int page;
    int pageSize;
}
