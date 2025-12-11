package org.example.backend.post;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class HateSpeechException extends RuntimeException {
    public HateSpeechException(String message) {
        super(message);
    }
}
