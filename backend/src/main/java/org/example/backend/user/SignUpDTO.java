package org.example.backend.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor

public class SignUpDTO {
    
    private String email;
    private String password;

}