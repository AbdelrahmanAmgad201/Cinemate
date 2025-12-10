package org.example.backend.post;

import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@Builder
public class AddPostDto {
    private ObjectId forumId;
    private String title;
    private String content;
}
