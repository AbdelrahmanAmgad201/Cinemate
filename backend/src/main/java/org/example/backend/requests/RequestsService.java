package org.example.backend.requests;

import lombok.RequiredArgsConstructor;
import org.example.backend.movie.Movie;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestsService {

    private final RequestsRepository requestsRepository;

    public void deleteOldRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(10);
        requestsRepository.deleteOldNonPending(cutoff);
        System.out.println("Old decline mails cleanup executed");
    }
    public Requests addRequest(Movie movie) {

        Requests request = Requests.builder()
                .movieName(movie.getName())
                .organization(movie.getOrganization())
                .movie(movie)
                .state(State.PENDING)
                .build();

        return requestsRepository.save(request);
    }
    public List<Requests> getAllPendingRequests() {
        return requestsRepository.findAllByState(State.PENDING);
    }
    public List<Requests> getAllOrganizationRequests(Long organizationId) {
        return requestsRepository.findAllByOrganization_Id(organizationId);
    }
}
