package org.example.backend.admin;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieRepository;
import org.example.backend.requests.Requests;
import org.example.backend.requests.RequestsRepository;
import org.example.backend.requests.State;
import org.example.backend.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;


@Service
@RequiredArgsConstructor
public class AdminService {
    private final MovieRepository movieRepository;
    private final RequestsRepository requestsRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Movie getRequestedMovie(Long requestId) {
        Requests request = requestsRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        return request.getMovie();
    }

    @Transactional
    public void acceptRequests(Long adminId,Long requestId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        Requests requests = requestsRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        Movie movie = requests.getMovie();
        requests.setState(State.ACCEPTED);
        requests.setAdmin(admin);
        requests.setStateUpdatedAt(LocalDateTime.now());
        requestsRepository.save(requests);
        movie.setAdmin(admin);
        movieRepository.save(movie);
    }

    @Transactional
    public void declineRequest(Long adminId,Long requestId) {
        Requests requests = requestsRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        Long movieId = requests.getMovie().getMovieID();
        requests.setState(State.REJECTED);
        requests.setMovie(null);
        requests.setAdmin(admin);
        requests.setStateUpdatedAt(LocalDateTime.now());
        requestsRepository.save(requests);
        movieRepository.deleteById(movieId);
    }
    @Transactional
    public Admin addAdmin(AddAdminDTO addAdminDTO) {
        String email = addAdminDTO.getEmail();
        String password = passwordEncoder.encode(addAdminDTO.getPassword());
        Admin admin = Admin.builder()
                .email(email)
                .password(password)
                .build();
        return adminRepository.save(admin);
    }
    @Transactional
    public SystemOverview getSystemOverview() {
        SystemOverview systemOverview = new SystemOverview();

        systemOverview.setNumberOfUsers(userRepository.count());
        systemOverview.setNumberOfMovies(movieRepository.countByAdminIsNotNull());

        var popularOrgList = movieRepository.getMostPopularOrganization(PageRequest.of(0,1));
        systemOverview.setMostPopularOrganization(popularOrgList.isEmpty() ? null : popularOrgList.get(0));

        var mostRatedList = movieRepository.getMostRatedMovie(PageRequest.of(0,1));
        systemOverview.setMostRatedMovie(mostRatedList.isEmpty() ? null : mostRatedList.get(0));

        var mostLikedList = movieRepository.getMostLikedMovie(PageRequest.of(0,1));
        systemOverview.setMostLikedMovie(mostLikedList.isEmpty() ? null : mostLikedList.get(0));

        return systemOverview;
    }
}
