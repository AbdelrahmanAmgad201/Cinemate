package org.example.backend.forum;

import java.util.UUID;

public interface ForumDisplayDTO {

    UUID getId();

    String getName();

    String getDescription();

    Integer getFollowerCount();
}
