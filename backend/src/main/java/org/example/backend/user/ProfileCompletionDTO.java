package org.example.backend.user;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileCompletionDTO {
    private LocalDate birthday;
    private String gender;
}
