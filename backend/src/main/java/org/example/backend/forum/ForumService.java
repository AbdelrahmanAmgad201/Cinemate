package org.example.backend.forum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.hateSpeach.HateSpeachService;
import org.example.backend.hateSpeach.HateSpeechException;
import org.example.backend.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumService {

    private static final int BATCH_SIZE = 100;

    private final ForumRepository forumRepository;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    private final MongoTemplate mongoTemplate;
    private final HateSpeachService hateSpeachService;

    public Forum createForum(ForumCreationRequest request, Long userId) {
        if (!hateSpeachService.analyzeText(request.getName())||!hateSpeachService.analyzeText(request.getDescription())) {
            throw new HateSpeechException("hate speech detected");
        }

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

        if (!hateSpeachService.analyzeText(request.getName())||!hateSpeachService.analyzeText(request.getDescription())) {
            throw new HateSpeechException("hate speech detected");
        }

        String oldName = forum.getName();

        forum.setName(request.getName());
        forum.setDescription(request.getDescription());

        if (! forum.getName().equals(oldName)) updatePostsForumName(forum);

        return forumRepository.save(forum);
    }

    @Async
    protected void updatePostsForumName(Forum forum) {
        String newName = forum.getName();
        try {
            // Get all post IDs for this user
            List<ObjectId> postIds = getIds("posts", Criteria.where("forumId").is(forum.getId()));

            if (postIds.isEmpty()) {
                return;
            }
            int totalPosts = ChangePostsAuthor (postIds, newName);
        } catch (Exception e) {
            log.error("Error during posts cascade updating userName: {}",forum.getId(), e);
        }
    }

    private List<ObjectId> getIds(String collection, Criteria criteria) {
        Query query = new Query(criteria);
        List<ObjectId> ids = mongoTemplate.findDistinct(
                query,
                "_id",
                collection,
                ObjectId.class
        );

        log.info("Found {} posts to change name for forum {}", ids.size(), criteria);
        return ids;
    }

    private int ChangePostsAuthor(List<ObjectId> postIds, String newName) {
        int totalChanged = 0;

        for (int i = 0; i < postIds.size(); i += BATCH_SIZE) {
            List<ObjectId> batch = postIds.subList(i, Math.min(i + BATCH_SIZE, postIds.size()));
            long changed = changeForumNameBatch("posts", Criteria.where("_id").in(batch), newName);
            totalChanged += changed;
            log.debug("change author name batch of {} posts", changed);
        }

        return totalChanged;
    }

    private long changeForumNameBatch(String collection, Criteria criteria, String newName) {
        Query query = new Query(criteria);
        Update update = new Update()
                .set("forumName", newName);

        return mongoTemplate.updateMulti(query, update, collection).getModifiedCount();
    }

    public SearchResultDto searchForums(String searchTerm, Pageable pageable) {
        Page<Forum> forumsPage = forumRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(
                searchTerm,
                pageable
        );
        return buildSearchResult(forumsPage);
    }

    public Forum getForumById(ObjectId forumId) {
        Forum forum =mongoTemplate.findById(forumId, Forum.class);
        if (forum.getIsDeleted()) {
            throw new IllegalStateException("Cannot update a deleted forum");
        }
        return forum;
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

    public String getForumName(ObjectId forumId) {
        Forum forum = mongoTemplate.findById(forumId, Forum.class);
        if (forum == null) {
            throw new IllegalArgumentException("Forum not found with id: " + forumId);
        }
        return forum.getName();
    }

    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}

