package org.example.backend.verification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor

public class VerificationDTO {
    private String email;
    private int code;
}
