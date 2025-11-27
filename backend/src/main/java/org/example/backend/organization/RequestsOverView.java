package org.example.backend.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class RequestsOverView {
    private Long numberOfPendings;
    private Long numberOfRejected;
    private Long numberOfAccepted;
}
