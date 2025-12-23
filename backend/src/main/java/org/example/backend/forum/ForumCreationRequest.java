package org.example.backend.forum;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForumCreationRequest {
    @NotBlank
    private String name;
    @NotBlank
    public String description;
}
