package org.example.backend.watchparty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WatchPartyUserDTO {
    Long userId;
    String userName;
}
