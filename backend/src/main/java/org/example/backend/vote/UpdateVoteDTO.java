package org.example.backend.vote;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class UpdateVoteDTO {
    @NotNull
    private ObjectId targetId;

    @NotNull
    private Integer value;
}
