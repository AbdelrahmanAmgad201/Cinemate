package org.example.backend.requests;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RequestsResponse {
    private Long id;
    private String movieName;
    private LocalDateTime createdAt;
    private LocalDateTime stateUpdatedAt;
    private State state;
    private String organization;
    private Long movieId;
    private String admin;

    public static RequestsResponse from(Requests request) {
        return RequestsResponse.builder()
                .id(request.getId())
                .movieName(request.getMovieName())
                .createdAt(request.getCreatedAt())
                .stateUpdatedAt(request.getStateUpdatedAt())
                .state(request.getState())
                .organization(request.getOrganizationName())
                .movieId(request.getMovieId())
                .admin(request.getAdminName())
                .build();
    }
}
