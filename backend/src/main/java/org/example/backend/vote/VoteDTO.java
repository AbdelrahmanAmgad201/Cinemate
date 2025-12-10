package org.example.backend.vote;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class VoteDTO {
    private String targetId;
    private Integer value;
}
