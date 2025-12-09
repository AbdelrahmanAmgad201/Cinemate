package org.example.backend.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddAdminDTO {
    private String name;
    private String email;
    private String password;
}
