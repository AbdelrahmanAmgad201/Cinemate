package org.example.backend.organization;

import jakarta.transaction.Transactional;
import org.example.backend.movie.Movie;
import org.example.backend.movie.MovieAddDTO;
import org.example.backend.movie.MovieRepository;
import org.example.backend.movie.MovieService;
import org.example.backend.requests.RequestsService;
import org.example.backend.user.Gender;
import org.example.backend.user.User;
import org.example.backend.user.UserDataDTO;
import org.example.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrganizationService {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private RequestsService requestsService;
    @Autowired
    private MovieService movieService;

    @Transactional
    public Organization addOrganization(String email, String password) {
        Organization organization = Organization.builder()
                .email(email)
                .password(password)
                .build();
        return organizationRepository.save(organization);
    }


    @Transactional
    public String setOrganizationData(Long userId, OrganizationDataDTO organizationDataDTO) {
        Organization organization = organizationRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        organization.setName(organizationDataDTO.getName());
        organization.setAbout(organizationDataDTO.getAbout());

        organizationRepository.save(organization);

        return "User data updated successfully";
    }

    @Transactional
    public Long requestMovie(Long orgId,MovieAddDTO movieAddDTO) {

        Movie savedMovie = movieService.addMovie(orgId,movieAddDTO);
        requestsService.addRequest(savedMovie);
        return savedMovie.getMovieID();
    }

    public PersonalData getPersonalData(Long userId) {
        return organizationRepository.findProjectedById(userId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    public void updateAbout(Long userId, AboutDTO aboutDTO) {
        String about = aboutDTO.getAbout();
        Organization organization = organizationRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        organization.setAbout(about);
        organizationRepository.save(organization);
    }

    public void updateName(Long userId, String newName) {
        Organization organization = organizationRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        organization.setName(newName);
        organizationRepository.save(organization);
    }

}
