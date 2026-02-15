package org.example.backend.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
public class UserProfileResponseDTO {
    private String firstName;
    private String lastName;
    private Integer numberOfFollowing;
    private Integer numberOfFollowers;
    private LocalDateTime createdAt;
    private String aboutMe;
    private LocalDate birthDate;
    private Gender gender;
}
