package org.example.backend.post;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MainFeedRequestDTO {
    @Min(0)
    int page;

    @Min(1)
    @Max(100)
    int pageSize;
}
