package org.example.backend.forum;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;

    public Forum createForum(ForumCreationRequest request, Long userId) {

        Forum forum = Forum.builder()
                .ownerId(longToObjectId(userId))
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(Instant.now())
                .build();

        return forumRepository.save(forum);
    }


    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}

