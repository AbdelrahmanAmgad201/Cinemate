package org.example.backend.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BirthDateDTO {
    @NotNull
    @Past
    private LocalDate birthDate;
}
