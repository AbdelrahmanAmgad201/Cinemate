package org.example.backend.common.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AboutDTO {
    @Size(max = 2000)
    private String about;
}
