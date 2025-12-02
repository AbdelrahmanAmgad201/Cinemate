package org.example.backend.forum;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "forums")
public class Forum {

    @Id
    private ObjectId id;

    private String name;
    private String description;

    @Indexed
    private ObjectId ownerId;

    @Builder.Default
    private Integer followerCount = 0;

    @Builder.Default
    private Integer postCount = 0;

    private Instant createdAt;

    @Builder.Default
    private Boolean isDeleted = false;

    private Instant deletedAt;
}