package org.example.backend.forum;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    private final MongoTemplate mongoTemplate;

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

    public Forum updateForum(ObjectId forumId, ForumCreationRequest request, long userId) {
        Forum forum = mongoTemplate.findById(forumId, Forum.class);

        if (forum == null) {
            throw new IllegalArgumentException("Forum not found with id: " + forumId);
        }

        if (forum.getIsDeleted()) {
            throw new IllegalStateException("Cannot update a deleted forum");
        }

        if (!forum.getOwnerId().equals(longToObjectId(userId))) {
            throw new AccessDeniedException("User does not have permission to update this forum");
        }

        forum.setName(request.getName());
        forum.setDescription(request.getDescription());

        return forumRepository.save(forum);
    }

    public SearchResultDto searchForums(String searchTerm, Pageable pageable) {
        Page<Forum> forumsPage = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                searchTerm,
                pageable
        );
        return buildSearchResult(forumsPage);
    }
    private SearchResultDto buildSearchResult(Page<Forum> page) {
        return SearchResultDto.builder()
                .forums(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }


    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}

