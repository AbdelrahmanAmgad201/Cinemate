package org.example.backend.comment;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;
import org.example.backend.mongo.SoftDeletableDocument;
import org.example.backend.vote.Votable;
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
@SuperBuilder
@Document(collection = "comments")
@CompoundIndexes({
        @CompoundIndex(name = "post_created", def = "{'postId': 1, 'isDeleted': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "post_score", def = "{'postId': 1, 'isDeleted': 1, 'score': -1}"),
        @CompoundIndex(name = "parent_created", def = "{'parentId': 1, 'isDeleted': 1, 'createdAt': 1}")
})
public class Comment extends SoftDeletableDocument implements Votable {

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

    // Denormalized from the parent Post at creation time (PERF-06) so
    // AccessService.canDeleteComment() can check post/forum ownership without chasing
    // Comment -> Post -> Forum on every call. Null on comments created before this field
    // existed; canDeleteComment() falls back to the old lookup chain for those.
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId postOwnerId;

    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId forumId;

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

    @Builder.Default
    private Integer numberOfReplies = 0;

    @Override
    public void incrementUpvote() {
        this.upvoteCount++;
    }
    @Override
    public void incrementDownvote() {
        this.downvoteCount++;
    }
    @Override
    public void decrementUpvote() {
        this.upvoteCount--;
    }
    @Override
    public void decrementDownvote() {
        this.downvoteCount--;
    }
    @Override
    public void updateScore(){
        this.score = this.upvoteCount - this.downvoteCount;
    }
}