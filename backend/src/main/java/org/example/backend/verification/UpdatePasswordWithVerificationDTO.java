package org.example.backend.verification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordWithVerificationDTO {
    @NotBlank
    @Size(min = 8)
    private String password;

    @Min(100000)
    @Max(999999)
    private int code;
}
