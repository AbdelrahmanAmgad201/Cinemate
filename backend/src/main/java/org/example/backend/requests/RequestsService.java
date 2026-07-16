package org.example.backend.requests;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.movie.Movie;
import org.example.backend.organization.RequestsOverView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestsService {

    // Cap on these admin/org dashboard lists (API-NEW-01) — they're "show me
    // everything relevant" views, not paged browsers, so a fixed ceiling instead of
    // full page-by-page navigation is the right fit here.
    private static final int MAX_LIST_SIZE = 200;
    private static final Pageable LATEST_FIRST =
            PageRequest.of(0, MAX_LIST_SIZE, Sort.by(Sort.Direction.DESC, "id"));

    private final RequestsRepository requestsRepository;
    private final Clock clock;
    @Transactional
    public void deleteOldRequests() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(10);
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
    @Transactional(readOnly = true)
    public List<RequestsResponse> getAllPendingRequests() {
        return toResponses(requestsRepository.findAllByState(State.PENDING, LATEST_FIRST));
    }
    @Transactional(readOnly = true)
    public List<RequestsResponse> getAllOrganizationRequests(Long organizationId) {
        return toResponses(requestsRepository.findAllByOrganization_Id(organizationId, LATEST_FIRST));
    }
    @Transactional(readOnly = true)
    public List<RequestsResponse> getAllAdminRequests(Long adminId) {
        return toResponses(requestsRepository.findByAdmin_Id(adminId, LATEST_FIRST));
    }

    private static List<RequestsResponse> toResponses(List<Requests> requests) {
        return requests.stream().map(RequestsResponse::from).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public RequestsOverView getRequestsOverView(Long orgId) {
        return requestsRepository.getRequestsOverviewByOrgId(orgId);
    }
}
