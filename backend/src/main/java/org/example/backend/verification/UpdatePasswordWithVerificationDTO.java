package org.example.backend.verification;

import lombok.Data;

@Data
public class UpdatePasswordWithVerificationDTO {
    private String password;
    private int code;
}
