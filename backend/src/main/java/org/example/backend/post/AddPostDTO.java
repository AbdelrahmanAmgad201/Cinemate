package org.example.backend.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AddPostDTO {
    // Not @NotNull: this DTO is shared with updatePost(), which doesn't use forumId
    // at all (only addPost() does, where the service already 404s on a missing forum).
    private UUID forumId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
