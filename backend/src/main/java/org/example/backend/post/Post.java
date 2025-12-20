package org.example.backend.post;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.bson.types.ObjectId;
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
@Builder
@Document(collection = "posts")
@CompoundIndexes({
        @CompoundIndex(name = "forum_created", def = "{'forumId': 1, 'isDeleted': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "forum_score", def = "{'forumId': 1, 'isDeleted': 1, 'score': -1}"),
        @CompoundIndex(name = "forum_hot", def = "{'forumId': 1, 'isDeleted': 1, 'lastActivityAt': -1}"),
        // NEW: Index for explore feed
        @CompoundIndex(name = "explore_popular", def = "{'isDeleted': 1, 'createdAt': -1, 'score': -1}")
})
public class Post implements Votable {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId forumId;

    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId ownerId;

    private String title;
    private String content;
    private String forumName;
    private String authorName;

    @Builder.Default
    private Integer upvoteCount = 0;

    @Builder.Default
    private Integer downvoteCount = 0;

    @Builder.Default
    private Integer score = 0;

    @Builder.Default
    private Integer commentCount = 0;

    private Instant createdAt;
    private Instant lastActivityAt;

    @Builder.Default
    private Boolean isDeleted = false;

    private Instant deletedAt;


    public void updateLastActivityAt(Instant lastActivityAt) {
        if (this.lastActivityAt == null || this.lastActivityAt.isBefore(lastActivityAt)) {
            this.lastActivityAt = lastActivityAt;
        }
    }
    @Override
    public void incrementUpvote() {
        updateLastActivityAt(Instant.now());
        this.upvoteCount++;
    }
    @Override
    public void incrementDownvote() {
        updateLastActivityAt(Instant.now());
        this.downvoteCount++;
    }
    @Override
    public void decrementUpvote() {
        updateLastActivityAt(Instant.now());
        this.upvoteCount--;
    }
    @Override
    public void decrementDownvote() {
        updateLastActivityAt(Instant.now());
        this.downvoteCount--;
    }
    @Override
    public void updateScore(){
        this.score = this.upvoteCount - this.downvoteCount;
    }
}