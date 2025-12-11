package org.example.backend.post;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class ForumPostsRequestDTO {
    private int page;
    private int pageSize;
    private ObjectId forumId;
}
