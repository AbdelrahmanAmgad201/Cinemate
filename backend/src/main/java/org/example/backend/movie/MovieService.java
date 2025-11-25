package org.example.backend.movie;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsRepository;
import org.example.backend.requests.State;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final OrganizationRepository organizationRepository;
    private final RequestsRepository requestsRepository;

    @Transactional
    public Page<Movie> getMovies(MovieRequestDTO movieRequestDTO) {
        Specification<Movie> spec = MovieSpecification.filterMovies(movieRequestDTO);

        Pageable pageable = PageRequest.of(
                movieRequestDTO.getPage(),
                movieRequestDTO.getPageSize()
        );

        return movieRepository.findAll(spec, pageable);
    }

    @Transactional
    public Movie addMovie(Long orgId,MovieAddDTO movieAddDTO) {

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Movie movie = Movie.builder()
                .name(movieAddDTO.getName())
                .description(movieAddDTO.getDescription())
                .movieUrl(movieAddDTO.getMovieUrl())
                .thumbnailUrl(movieAddDTO.getThumbnailUrl())
                .trailerUrl(movieAddDTO.getTrailerUrl())
                .duration(movieAddDTO.getDuration())
                .genre(movieAddDTO.getGenre())
                .organization(organization)
                .build();

        return movieRepository.save(movie);
    }

    @Transactional
    public Movie getMovie(Long id){
        return movieRepository.findById(id).orElseThrow(() -> new RuntimeException("Movie not found"));
    }
}
