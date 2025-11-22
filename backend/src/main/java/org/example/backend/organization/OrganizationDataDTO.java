package org.example.backend.organization;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrganizationDataDTO {
    private String name;
    private String about;
}
