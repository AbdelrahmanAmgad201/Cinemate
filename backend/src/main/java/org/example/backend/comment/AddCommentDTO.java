package org.example.backend.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@Builder
public class AddCommentDTO {
    private ObjectId parentId;

    @NotNull
    private ObjectId postId;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
