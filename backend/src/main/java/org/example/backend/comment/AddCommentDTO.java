package org.example.backend.comment;

import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@Builder
public class AddCommentDTO {
    private String parentId;
    private String postId;
    private String content;
}
