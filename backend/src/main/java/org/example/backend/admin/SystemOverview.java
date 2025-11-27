package org.example.backend.admin;

import lombok.Getter;
import lombok.Setter;
import org.example.backend.organization.Organization;

@Getter
@Setter
public class SystemOverview {
    private Long numberOfUsers;
    private Long numberOfMovies;
    private Organization mostPopularOrganization;
    private Long mostLikedMovie;
    private Long mostRatedMovie;
}
