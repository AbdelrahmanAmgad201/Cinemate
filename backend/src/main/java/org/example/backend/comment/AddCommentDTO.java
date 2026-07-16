package org.example.backend.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AddCommentDTO {
    private UUID parentId;

    @NotNull
    private UUID postId;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
