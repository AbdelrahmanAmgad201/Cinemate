package org.example.backend.post;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "posts")
@CompoundIndexes({
        @CompoundIndex(name = "forum_created", def = "{'forumId': 1, 'isDeleted': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "forum_score", def = "{'forumId': 1, 'isDeleted': 1, 'score': -1}"),
        @CompoundIndex(name = "forum_hot", def = "{'forumId': 1, 'isDeleted': 1, 'lastActivityAt': -1}")
})
public class Post {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId forumId;

    @Indexed
    private ObjectId ownerId;

    private String title;
    private String content;

    @Builder.Default
    private Integer upvoteCount = 0;

    @Builder.Default
    private Integer downvoteCount = 0;

    @Builder.Default
    private Integer score = 0;  // upvotes - downvotes

    @Builder.Default
    private Integer commentCount = 0;

    // Timestamps
    private Instant createdAt;
    private Instant lastActivityAt;  // Updated when comment added

    @Builder.Default
    private Boolean isDeleted = false;

    private Instant deletedAt;
}