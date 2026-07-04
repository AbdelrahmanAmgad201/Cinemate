package org.example.backend.vote;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@Builder
public class VoteDTO {
    @NotNull
    private ObjectId targetId;

    @NotNull
    private Integer value;
}
