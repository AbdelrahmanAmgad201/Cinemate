package org.example.backend.movie;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;

    @Transactional
    public Page<Movie> getMovies(MovieRequestDTO movieRequestDTO) {
        Specification<Movie> spec = MovieSpecification.filterMovies(movieRequestDTO);

        Pageable pageable = PageRequest.of(
                movieRequestDTO.getPage(),
                movieRequestDTO.getPageSize()
        );

        return movieRepository.findAll(spec, pageable);
    }
}
