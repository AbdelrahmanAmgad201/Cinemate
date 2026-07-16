package org.example.backend.movie;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.organization.MoviesOverview;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationRepository;
import org.example.backend.requests.RequestsRepository;
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

    @Transactional(readOnly = true)
    public Page<MovieDetailsDTO> getMovies(MovieRequestDTO movieRequestDTO) {
        Specification<Movie> spec = MovieSpecification.filterMovies(movieRequestDTO)
                .and((root, query, cb) -> cb.isNotNull(root.get("admin")));

        Pageable pageable = PageRequest.of(
                movieRequestDTO.getPage(),
                movieRequestDTO.getPageSize()
        );

        // Mapped to a DTO here, inside the transaction (CQ-NEW-03) — Specification-based
        // queries don't support Spring Data's automatic interface projections, so this
        // maps explicitly instead of using a projection interface like MovieView.
        return movieRepository.findAll(spec, pageable).map(MovieDetailsDTO::from);
    }

    @Transactional
    public Movie addMovie(Long orgId,MovieAddDTO movieAddDTO) {

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

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

    @Transactional(readOnly = true)
    public MovieDetailsDTO getMovie(Long id){
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        if (movie.getAdmin() == null) {
            throw new ResourceNotFoundException("Movie not found");
        }

        // Mapped to a DTO here, inside the transaction, since MovieDetailsDTO.from()
        // touches the lazy organization association (CQ-NEW-03).
        return MovieDetailsDTO.from(movie);
    }

    @Transactional(readOnly = true)
    public MoviesOverview getMoviesOverview(Long orgId){
        MovieCountAndViewsDTO counts = movieRepository.getMovieCountAndTotalViews(orgId);
        int numberOfMovies = (int) counts.numberOfMovies();
        int totalViews = (int) counts.totalViews();

        List<Object[]> genreViews = movieRepository.getGenresOrderedByViews(orgId);

        Genre mostPopularGenre = genreViews.isEmpty()
                ? null
                : (Genre) genreViews.get(0)[0];

        return new MoviesOverview(
                numberOfMovies,
                totalViews,
                mostPopularGenre
        );
    }

    @Transactional(readOnly = true)
    public OneMovieOverView getMovieStatsByMovieId(Long movieId){
        return movieRepository.getMovieOverview(movieId);
    }

    @Transactional(readOnly = true)
    public boolean OrganizationOwnMovie(Long orgId, Long movieId){
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new ResourceNotFoundException("Movie not found"));
        return movie.getOrganization().getId().equals(orgId);
    }

    public Page<MovieView> getOrganizationMovies(Long orgId, Pageable pageable){
        return movieRepository.findAllByAdminIsNotNullAndOrganization_Id(orgId, pageable);
    }
}
