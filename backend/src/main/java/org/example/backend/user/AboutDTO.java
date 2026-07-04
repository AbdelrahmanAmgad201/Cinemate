package org.example.backend.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AboutDTO {
    @Size(max = 2000)
    private String about;
}
