package org.example.backend.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileCompletionDTO {
    @NotNull
    @Past
    private LocalDate birthday;

    @NotNull
    private String gender;
}
