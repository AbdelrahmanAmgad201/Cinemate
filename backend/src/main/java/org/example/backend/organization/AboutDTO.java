package org.example.backend.organization;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AboutDTO {
    @Size(max = 2000)
    String about;
}
