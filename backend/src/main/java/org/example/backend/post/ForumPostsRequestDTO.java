package org.example.backend.post;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ForumPostsRequestDTO {
    @Min(0)
    private int page;

    @Min(1)
    @Max(100)
    private int pageSize;

    @NotNull
    private UUID forumId;

    private String sortBy; // new - old - top
}
