package org.example.backend.vote;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;
import org.example.backend.mongo.SoftDeletableDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Document(collection = "votes")
@CompoundIndexes({
        @CompoundIndex(name = "user_target_unique", def = "{'userId': 1, 'targetType': 1, 'targetId': 1}", unique = true),
        @CompoundIndex(name = "target_type", def = "{'targetType': 1, 'targetId': 1, 'voteType': 1}")
})
public class Vote extends SoftDeletableDocument {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;

    private VoteTargetType targetType;  // whether targetId refers to a Post or a Comment
    private ObjectId targetId;

    private Integer voteType;  // 1 = upvote, -1 = downvote

    private Instant createdAt;
}