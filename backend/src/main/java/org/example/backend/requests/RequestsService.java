package org.example.backend.requests;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.movie.Movie;
import org.example.backend.organization.RequestsOverView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestsService {

    private final RequestsRepository requestsRepository;
    @Transactional
    public void deleteOldRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(10);
        requestsRepository.deleteOldNonPending(cutoff);
        log.info("Old declined requests cleanup executed — removed entries before {}", cutoff);

    }
    @Transactional
    public Requests addRequest(Movie movie) {

        Requests request = Requests.builder()
                .movieName(movie.getName())
                .organization(movie.getOrganization())
                .movie(movie)
                .state(State.PENDING)
                .build();

        return requestsRepository.save(request);
    }
    @Transactional
    public List<Requests> getAllPendingRequests() {
        return requestsRepository.findAllByState(State.PENDING);
    }
    @Transactional
    public List<Requests> getAllOrganizationRequests(Long organizationId) {
        return requestsRepository.findAllByOrganization_Id(organizationId);
    }
    @Transactional
    public List<Requests> getAllAdminRequests(Long adminId) {
        return requestsRepository.findByAdmin_Id(adminId);
    }
    @Transactional
    public RequestsOverView getRequestsOverView(Long orgId) {
        return requestsRepository.getRequestsOverviewByOrgId(orgId);
    }
}
