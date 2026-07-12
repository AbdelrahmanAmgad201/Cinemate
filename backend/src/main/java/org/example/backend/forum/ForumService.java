package org.example.backend.forum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.moderation.ContentType;
import org.example.backend.moderation.ModerationOutboxService;
import org.example.backend.moderation.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    private final ModerationOutboxService moderationOutboxService;

    // Optimistic-publish moderation: forum visible immediately as PENDING; name+description
    // moderated together; the insert + outbox entry commit in one transaction.
    @Transactional
    public ForumDetailsDTO createForum(ForumCreationRequest request, Long userId) {
        Forum forum = Forum.builder()
                .ownerId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(Instant.now())
                .build();

        Forum saved = forumRepository.save(forum);
        moderationOutboxService.enqueue(ContentType.FORUM, saved.getId(),
                saved.getModerationVersion(), moderationText(saved.getName(), saved.getDescription()));
        return ForumDetailsDTO.from(saved);
    }

    // name + description moderated as one text snapshot — one pipeline round-trip.
    private static String moderationText(String name, String description) {
        return name + "\n" + description;
    }

    @Transactional
    public void deleteForum(UUID forumId, long userId) {
        if (!accessService.canDeleteForum(userId, forumId)) {
            throw new AccessDeniedException("User cannot delete this forum");
        }
        deletionService.deleteForum(forumId);
    }

    /** Moderation-driven removal (ModerationVerdictConsumer) — no ownership check. */
    @Transactional
    public void systemDeleteForum(UUID forumId) {
        deletionService.deleteForum(forumId);
    }

    @Transactional
    public Forum updateForum(UUID forumId, ForumCreationRequest request, long userId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new IllegalArgumentException("Forum not found with id: " + forumId));
        if (Boolean.TRUE.equals(forum.getIsDeleted())) {
            throw new IllegalStateException("Cannot update a deleted forum");
        }
        if (!forum.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to update this forum");
        }

        forum.setName(request.getName());
        forum.setDescription(request.getDescription());
        long newVersion = forum.getModerationVersion() + 1;
        forum.setModerationVersion(newVersion);
        forum.setModerationStatus(ModerationStatus.PENDING);
        Forum saved = forumRepository.save(forum);
        moderationOutboxService.enqueue(ContentType.FORUM, saved.getId(),
                newVersion, moderationText(saved.getName(), saved.getDescription()));
        // No forum-name denormalization to sync onto posts anymore — the name is joined.
        return saved;
    }

    @Transactional(readOnly = true)
    public SearchResultDTO searchForums(String searchTerm, Pageable pageable) {
        Page<Forum> forumsPage = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(searchTerm, pageable);
        return buildSearchResult(forumsPage);
    }

    @Transactional(readOnly = true)
    public Page<ForumDisplayDTO> findUserForums(Long userId, Pageable pageable) {
        return forumRepository.findAllByOwnerIdAndIsDeletedFalse(userId, pageable);
    }

    @Transactional(readOnly = true)
    public ForumDetailsDTO getForumById(UUID forumId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResourceNotFoundException("Forum not found with id: " + forumId));
        if (Boolean.TRUE.equals(forum.getIsDeleted())) {
            throw new IllegalStateException("Cannot access a deleted forum");
        }
        return ForumDetailsDTO.from(forum);
    }

    private SearchResultDTO buildSearchResult(Page<Forum> page) {
        return SearchResultDTO.builder()
                .forums(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public String getForumName(UUID forumId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new IllegalArgumentException("Forum not found with id: " + forumId));
        return forum.getName();
    }
}
