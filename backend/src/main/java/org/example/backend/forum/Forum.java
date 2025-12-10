package org.example.backend.forum;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @NonNull
    @TextIndexed
    private String name;

    @NonNull
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