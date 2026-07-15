package org.example.watchparty.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * The slice of the backend's movie catalog this service needs to host a party — chiefly
 * the playable {@code movieUrl} (Wistia id). Deserialized from the backend's
 * {@code MovieDetailsDTO}; unknown fields are ignored so the two can evolve independently.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieMetadata {
    private Long movieID;
    private String name;
    private String movieUrl;
}
