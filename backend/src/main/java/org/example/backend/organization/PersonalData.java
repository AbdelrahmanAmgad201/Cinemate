package org.example.backend.organization;

import java.time.LocalDateTime;

public interface PersonalData {
    String getName();
    String getEmail();
    String getAbout();
    LocalDateTime getCreatedAt();

}
