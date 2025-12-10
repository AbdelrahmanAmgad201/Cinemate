package org.example.backend.comment;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
@Document(collection = "comments")
@CompoundIndexes({
        @CompoundIndex(name = "post_created", def = "{'postId': 1, 'isDeleted': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "post_score", def = "{'postId': 1, 'isDeleted': 1, 'score': -1}"),
        @CompoundIndex(name = "parent_created", def = "{'parentId': 1, 'isDeleted': 1, 'createdAt': 1}")
})
public class Comment {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId postId;

    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId parentId;  // null for top-level comments

    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId ownerId;

    private String content;

    @Builder.Default
    private Integer upvoteCount = 0;

    @Builder.Default
    private Integer downvoteCount = 0;

    @Builder.Default
    private Integer score = 0;

    @Builder.Default
    private Integer depth = 0;  // 0 = top-level, 1 = reply, 2 = nested reply

    private Instant createdAt;

    // Soft delete
    @Builder.Default
    private Boolean isDeleted = false;

    private Instant deletedAt;
}