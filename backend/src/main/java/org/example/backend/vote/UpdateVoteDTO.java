package org.example.backend.vote;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateVoteDTO {
    @NotNull
    private UUID targetId;

    @NotNull
    private Integer value;
}
