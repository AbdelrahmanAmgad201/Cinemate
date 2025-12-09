package org.example.backend.forum;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;

    public Forum createForum(ForumCreationRequest request, Long userId) {

        Forum forum = Forum.builder()
                .ownerId(longToObjectId(userId))
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(Instant.now())
                .build();

        return forumRepository.save(forum);
    }

    public void deleteForum(ObjectId forumId, long userId) {
        if (!accessService.canDeleteForum(longToObjectId(userId), forumId)) {
            throw new AccessDeniedException("User " + " cannot delete this forum");
        }

        deletionService.deleteForum(forumId);
    }



    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}

