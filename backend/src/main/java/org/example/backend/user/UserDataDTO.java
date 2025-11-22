package org.example.backend.user;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDataDTO {
    private String firstName;
    private String lastName;
    private String gender;
    private String about;
    private LocalDate birthday;
}
