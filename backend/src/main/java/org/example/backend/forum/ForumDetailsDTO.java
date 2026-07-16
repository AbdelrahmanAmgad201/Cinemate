package org.example.backend.forum;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Single-forum detail view (CQ-NEW-03) — returned instead of the {@link Forum} entity
 * so create/get-by-id responses don't leak internal fields (isDeleted, deletedAt).
 */
@Data
@Builder
public class ForumDetailsDTO {
    private String id;
    private String name;
    private String description;
    private String ownerId;
    private Integer followerCount;
    private Integer postCount;
    private Instant createdAt;

    public static ForumDetailsDTO from(Forum forum) {
        return ForumDetailsDTO.builder()
                .id(forum.getId().toString())
                .name(forum.getName())
                .description(forum.getDescription())
                .ownerId(forum.getOwnerId().toString())
                .followerCount(forum.getFollowerCount())
                .postCount(forum.getPostCount())
                .createdAt(forum.getCreatedAt())
                .build();
    }
}
