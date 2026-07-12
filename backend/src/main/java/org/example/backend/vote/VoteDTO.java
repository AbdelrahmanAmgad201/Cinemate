package org.example.backend.vote;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VoteDTO {
    @NotNull
    private UUID targetId;

    @NotNull
    private Integer value;
}
