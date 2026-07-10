package org.example.backend.forum;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;
import org.example.backend.mongo.SoftDeletableDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Document(collection = "forums")
public class Forum extends SoftDeletableDocument {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @NonNull
    @TextIndexed
    private String name;

    @NonNull
    private String description;

    @Indexed
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId ownerId;

    @Builder.Default
    private Integer followerCount = 0;

    @Builder.Default
    private Integer postCount = 0;

    private Instant createdAt;
}