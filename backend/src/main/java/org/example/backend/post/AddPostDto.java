package org.example.backend.post;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
@Builder
public class AddPostDto {
    private ObjectId forumId;
    private String title;
    private String content;
}
