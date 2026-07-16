package org.example.backend.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrganizationDataDTO {
    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String about;
}
