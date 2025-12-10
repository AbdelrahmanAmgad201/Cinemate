package org.example.backend.comment;

import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@Builder
public class AddCommentDTO {
    private ObjectId parentId;
    private ObjectId postId;
    private String content;
}
