package org.example.backend.vote;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class UpdateVoteDTO {
    private ObjectId id;
    private Integer value;
}
