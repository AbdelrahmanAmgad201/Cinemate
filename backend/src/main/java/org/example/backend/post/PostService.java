package org.example.backend.post;

import lombok.RequiredArgsConstructor;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.example.backend.forumfollowing.FollowingRepository;
import org.example.backend.moderation.ContentType;
import org.example.backend.moderation.ModerationOutboxService;
import org.example.backend.moderation.ModerationStatus;
import org.example.backend.user.PrivateProfileException;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {
    private final ForumRepository forumRepository;
    private final FollowingRepository followingRepository;
    private final PostRepository postRepository;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    private final ModerationOutboxService moderationOutboxService;
    private final UserRepository userRepository;

    // Optimistic-publish moderation: the post is saved and visible immediately with
    // moderationStatus=PENDING; toxicity is checked asynchronously via the Kafka pipeline
    // and a flagged verdict removes it later. The post insert and the outbox entry commit
    // in one ordinary transaction; forum.post_count is maintained by a DB trigger.
    @Transactional
    public Post addPost(AddPostDTO addPostDto, Long userId) {
        Forum forum = forumRepository.findById(addPostDto.getForumId())
                .orElseThrow(() -> new ResourceNotFoundException("Forum not found"));
        if (Boolean.TRUE.equals(forum.getIsDeleted())) {
            throw new IllegalStateException("Forum has been deleted");
        }
        Instant now = Instant.now();
        Post post = Post.builder()
                .ownerId(userId)
                .forumId(addPostDto.getForumId())
                .title(addPostDto.getTitle())
                .content(addPostDto.getContent())
                .createdAt(now)
                .lastActivityAt(now)
                .build();

        Post saved = postRepository.save(post);
        moderationOutboxService.enqueue(ContentType.POST, saved.getId(),
                saved.getModerationVersion(), moderationText(saved.getTitle(), saved.getContent()));
        return saved;
    }

    @Transactional
    public Post updatePost(UUID postId, AddPostDTO addPostDto, Long userId) {
        Post post = postRepository.findById(postId).orElse(null);
        canUpdatePost(post, postId, userId);
        post.setTitle(addPostDto.getTitle());
        post.setContent(addPostDto.getContent());
        long newVersion = post.getModerationVersion() + 1;
        post.setModerationVersion(newVersion);
        post.setModerationStatus(ModerationStatus.PENDING);
        post.setModerationRequestedAt(Instant.now());
        Post saved = postRepository.save(post);
        moderationOutboxService.enqueue(ContentType.POST, postId,
                newVersion, moderationText(saved.getTitle(), saved.getContent()));
        return saved;
    }

    // Title + content moderated as a single text snapshot — one pipeline round-trip.
    private static String moderationText(String title, String content) {
        return title + "\n" + content;
    }

    @Transactional(readOnly = true)
    public Page<PostView> getForumPosts(ForumPostsRequestDTO forumPostsRequestDTO) {
        Sort sort = PostUtils.getSort(forumPostsRequestDTO.getSortBy());
        Pageable pageable = PageRequest.of(
                forumPostsRequestDTO.getPage(),
                forumPostsRequestDTO.getPageSize(),
                sort);
        return postRepository.findByIsDeletedFalseAndForumId(forumPostsRequestDTO.getForumId(), pageable);
    }

    private void canUpdatePost(Post post, UUID postId, Long userId) {
        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalStateException("Cannot update a deleted post");
        }
        if (!post.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to update this post");
        }
    }

    @Transactional
    public void deletePost(UUID postId, Long userId) {
        if (!accessService.canDeletePost(userId, postId)) {
            throw new AccessDeniedException("User cannot delete this post");
        }
        deletionService.deletePost(postId);
    }

    /**
     * Removes a post without an ownership check — for moderation-driven removal
     * (ModerationVerdictConsumer). The cascade soft-delete + DB triggers handle the
     * forum post-count decrement.
     */
    @Transactional
    public void systemDeletePost(Post post) {
        deletionService.deletePost(post.getId());
    }

    @Transactional(readOnly = true)
    public Page<PostView> getUserPosts(Long userId, MainFeedRequestDTO mainFeedRequestDTO) {
        Pageable pageable = PageRequest.of(
                mainFeedRequestDTO.getPage(),
                mainFeedRequestDTO.getPageSize());
        List<UUID> forumIds = followingRepository.findForumIdsByUserId(userId);
        if (forumIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return postRepository.findByIsDeletedFalseAndForumIdIn(forumIds, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PostView> getMyPosts(Long userId, Pageable pageable) {
        return postRepository.findAllByOwnerIdAndIsDeletedFalse(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PostView> getOtherUserPosts(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found with id: " + userId));
        if (user.getIsPublic()) {
            return postRepository.findAllByOwnerIdAndIsDeletedFalse(userId, pageable);
        }
        throw new PrivateProfileException("this profile is private");
    }

    @Transactional(readOnly = true)
    public PostView getPostById(UUID postId) {
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
    }
}
