package org.example.backend.verification;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "verfications")
public class Verfication {
    @Id
    @Column(name="email")
    private String email;
    @Column(name = "password")
    private String password;
    @Column(name = "code")
    private int code;

}
