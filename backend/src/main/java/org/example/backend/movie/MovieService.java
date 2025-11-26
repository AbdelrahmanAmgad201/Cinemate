package org.example.backend.movie;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.organization.MoviesOverview;
import org.example.backend.organization.OneMovieOverView;
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

    @Transactional
    public Page<Movie> getMovies(MovieRequestDTO movieRequestDTO) {
        Specification<Movie> spec = MovieSpecification.filterMovies(movieRequestDTO);

        Pageable pageable = PageRequest.of(
                movieRequestDTO.getPage(),
                movieRequestDTO.getPageSize()
        );

        return movieRepository.findAllByAdminIsNotNull(spec, pageable);
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
    @Transactional
    public MoviesOverview getMoviesOverview(Long orgId){
        Object resultObj = movieRepository.getMovieCountAndTotalViews(orgId);
        Object[] result = (Object[]) resultObj; // safe now
        int numberOfMovies = ((Number) result[0]).intValue();
        int totalViews = ((Number) result[1]).intValue();

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

    @Transactional
    public List<Movie> getOrganizationMovies(Long orgId){
        return movieRepository.findByAdminIsNotNullAndOrganization_Id(orgId);
    }

    @Transactional
    public OneMovieOverView getMovieStatsByMovieId(Long movieId){
        return movieRepository.getMovieOverview(movieId);
    }

    @Transactional
    public boolean OrganizationOwnMovie(Long orgId, Long movieId){
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
        return movie.getOrganization().getId().equals(orgId);
    }
}
