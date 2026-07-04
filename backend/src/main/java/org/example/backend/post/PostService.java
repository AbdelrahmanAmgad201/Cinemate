package org.example.backend.post;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.errorHandler.ResourceNotFoundException;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.example.backend.forumfollowing.FollowingRepository;
import org.example.backend.hateSpeech.HateSpeechService;
import org.example.backend.hateSpeech.HateSpeechException;
import org.example.backend.user.PrivateProfileException;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.example.backend.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

import java.time.Instant;

import static org.example.backend.util.IdConverter.longToObjectId;

@Service
@RequiredArgsConstructor
public class PostService {
    private final ForumRepository forumRepository;
    private final FollowingRepository followingRepository;

    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    private final HateSpeechService hateSpeechService;
    private final UserService userService;
    private final UserRepository userRepository;

    // No @Transactional here: this method operates on MongoDB documents (Forum, Post),
    // and Spring's @Transactional only covers the JPA/MySQL datasource — the annotation
    // had no real effect except implying atomicity that was never provided, while also
    // needlessly extending a MySQL connection's lifetime across the analyzeText() HTTP
    // call earlier in this method (HS-02).
    @CacheEvict(value = "exploreFeed", allEntries = true)
    public Post addPost(AddPostDTO addPostDto, Long userId) {
        Forum forum = mongoTemplate.findById(addPostDto.getForumId(), Forum.class);
        if (forum == null) {
            throw new ResourceNotFoundException("Forum not found");
        }
        if(forum.getIsDeleted()){
            throw new IllegalStateException("Forum has been deleted");
        }
        // Single combined call (HS-03/HS-07) instead of one per field: hate-api already
        // sentence-tokenizes internally, so title+content in one request has identical
        // detection coverage for half the HTTP round-trips, and removes the short-circuit
        // `||` ambiguity around partial-failure fail-open behavior the two-call version had.
        if (!hateSpeechService.analyzeText(addPostDto.getTitle() + "\n" + addPostDto.getContent())) {
            throw new HateSpeechException("hate speech detected");
        }
        ObjectId ObjectUserId = longToObjectId(userId);
        forum.setPostCount(forum.getPostCount() + 1);
        String ownerName = userService.getUserName(userId);
        forumRepository.save(forum);
        Instant now = Instant.now();
        Post post = Post.builder()
                .ownerId(ObjectUserId)
                .forumId(addPostDto.getForumId())
                .title(addPostDto.getTitle())
                .content(addPostDto.getContent())
                .createdAt(now)
                .forumName(forum.getName())
                .authorName(ownerName)
                .build();
        return (postRepository.save(post));
    }

    @Transactional
    @CacheEvict(value = "exploreFeed", allEntries = true)
    public Post updatePost(ObjectId postId, AddPostDTO addPostDto, Long userId) {
        if (!hateSpeechService.analyzeText(addPostDto.getTitle() + "\n" + addPostDto.getContent())) {
            throw new HateSpeechException("hate speech detected");
        }
        Post post = mongoTemplate.findById(postId, Post.class);
        canUpdatePost(post, postId, userId);
        post.setTitle(addPostDto.getTitle());
        post.setContent(addPostDto.getContent());
        return (postRepository.save(post));
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

    private void canUpdatePost(Post post, ObjectId postId, Long userId) {
        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }
        if (post.getIsDeleted()) {
            throw new IllegalStateException("Cannot update a deleted post");
        }
        if (!post.getOwnerId().equals(longToObjectId(userId))) {
            throw new AccessDeniedException("User does not have permission to update this forum");
        }
    }

    @Transactional
    @CacheEvict(value = "exploreFeed", allEntries = true)
    public void deletePost(ObjectId postId, Long userId) {
        if (!accessService.canDeletePost(longToObjectId(userId), postId)) {
            throw new AccessDeniedException("User " + " cannot delete this post");
        }
        Post post = mongoTemplate.findById(postId, Post.class);
        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }
        Forum forum = mongoTemplate.findById(post.getForumId(), Forum.class);
        if (forum == null) {
            throw new ResourceNotFoundException("Forum not found with id: " + post.getForumId());
        }
        forum.setPostCount(forum.getPostCount() - 1);
        forumRepository.save(forum);
        deletionService.deletePost(postId);
    }

    public Page<PostView> getUserPosts(Long userId, MainFeedRequestDTO mainFeedRequestDTO) {
        Pageable pageable = PageRequest.of(
                mainFeedRequestDTO.getPage(),
                mainFeedRequestDTO.getPageSize());
        List<ObjectId> forumIds = followingRepository.findForumIdsByUserId(longToObjectId(userId)).stream()
                .map(d -> d.getObjectId("forumId"))
                .toList();
        ;
        return postRepository.findByIsDeletedFalseAndForumIdIn(forumIds, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PostView> getMyPosts(Long userId, Pageable pageable) {
        ObjectId objectUserId = longToObjectId(userId);
        return getPostsByUserId(objectUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PostView> getOtherUserPosts(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found with id: " + userId));
        if(user.getIsPublic())
            return getPostsByUserId(longToObjectId(userId), pageable);

        throw new PrivateProfileException("this profile is private");
    }

    private Page<PostView> getPostsByUserId(ObjectId userId, Pageable pageable) {
        return postRepository.findAllByOwnerIdAndIsDeletedFalse(userId,pageable);
    }

    @Transactional(readOnly = true)
    public PostView getPostById(ObjectId postId) {
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
    }
}
