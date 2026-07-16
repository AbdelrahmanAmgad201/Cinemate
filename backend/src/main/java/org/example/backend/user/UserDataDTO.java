package org.example.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDataDTO {
    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    // gender/about/birthday stay unconstrained: UserService.updateUserData() already
    // treats a null gender as "leave unchanged" and tolerates null about/birthday.
    private String gender;

    @Size(max = 2000)
    private String about;

    @Past
    private LocalDate birthday;
}
