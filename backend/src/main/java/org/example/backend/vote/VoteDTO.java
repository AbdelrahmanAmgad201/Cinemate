package org.example.backend.vote;

import lombok.Builder;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@Builder
public class VoteDTO {
    private ObjectId targetId;
    private Integer value;
}
