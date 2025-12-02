package org.example.backend.forumFollow;

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
@Document(collection = "following")
@CompoundIndexes({
        @CompoundIndex(name = "user_forum_unique", def = "{'userId': 1, 'forumId': 1}", unique = true)
})
public class Following {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;

    @Indexed
    private ObjectId forumId;

    private Instant createdAt;

}