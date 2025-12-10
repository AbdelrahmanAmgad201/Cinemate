package org.example.backend.forum;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForumCreationRequest {
    @NotBlank
    private String name;
    @NotBlank
    public String description;
}
