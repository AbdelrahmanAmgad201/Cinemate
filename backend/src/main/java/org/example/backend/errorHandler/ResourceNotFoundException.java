package org.example.backend.errorHandler;

/**
 * Thrown when a requested entity does not exist. Mapped to HTTP 404 by
 * {@link GlobalExceptionHandler}, distinguishing "not found" from an
 * unexpected server error (which would otherwise both surface as a bare
 * {@link RuntimeException} and be reported as a 500).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
