package org.example.backend.admin;

import lombok.*;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RespondOnRequestDTO {
    private Long requestId;
    private Long adminId;
    }
