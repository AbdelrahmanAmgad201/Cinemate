package org.example.backend.vote;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
// DB-NEW-02: both indexes referenced a "targetType" field that doesn't exist on this
// document — the actual discriminator is "isPost" — so neither index was backing the
// queries it was meant to, silently falling back to a collection scan. Fixed to the
// real field name; the Boolean->enum conversion the audit also recommends is a larger,
// separate change (touches every call site plus existing data) left for later.
@CompoundIndexes({
        @CompoundIndex(name = "user_target_unique", def = "{'userId': 1, 'isPost': 1, 'targetId': 1}", unique = true),
        @CompoundIndex(name = "target_type", def = "{'isPost': 1, 'targetId': 1, 'voteType': 1}")
})
public class Vote {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;

    private Boolean isPost;  // post or comment
    private ObjectId targetId;

    private Integer voteType;  // 1 = upvote, -1 = downvote

    private Instant createdAt;


    @Builder.Default
    private Boolean isDeleted = false;

    private Instant deletedAt;
}