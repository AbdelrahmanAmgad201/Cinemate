package org.example.backend.vote;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "votes")
@CompoundIndexes({
        @CompoundIndex(name = "user_target_unique", def = "{'userId': 1, 'targetType': 1, 'targetId': 1}", unique = true),
        @CompoundIndex(name = "target_type", def = "{'targetType': 1, 'targetId': 1, 'voteType': 1}")
})
public class Vote {

    @Id
    private ObjectId id;

    private ObjectId userId;

    private Boolean isPost;  // post or comment
    private ObjectId targetId;

    private Integer voteType;  // 1 = upvote, -1 = downvote

    private Instant createdAt;


    @Builder.Default
    private Boolean isDeleted = false;

    private Instant deletedAt;
}