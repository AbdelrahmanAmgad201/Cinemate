package org.example.backend.post;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.example.backend.forumfollowing.FollowingRepository;
import org.example.backend.hateSpeach.HateSpeachService;
import org.example.backend.hateSpeach.HateSpeechException;
import org.example.backend.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PostService {
    private final RestTemplate restTemplate;
    private final ForumRepository forumRepository;
    private final FollowingRepository followingRepository;

    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    private final HateSpeachService hateSpeachService;
    private final UserService userService;

    @Transactional
    public Post addPost(AddPostDto addPostDto, Long userId) {
        Forum forum = mongoTemplate.findById(addPostDto.getForumId(), Forum.class);
        if (forum == null) {
            throw new RuntimeException("Forum not found");
        }
        if(forum.getIsDeleted()){
            throw new IllegalStateException("Forum has been deleted");
        }
        if (!hateSpeachService.analyzeText(addPostDto.getContent())||!hateSpeachService.analyzeText(addPostDto.getTitle())) {
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
    public Post updatePost(ObjectId postId, AddPostDto addPostDto, Long userId) {
        if (!hateSpeachService.analyzeText(addPostDto.getContent())||!hateSpeachService.analyzeText(addPostDto.getTitle())) {
            throw new HateSpeechException("hate speech detected");
        }
        Post post = mongoTemplate.findById(postId, Post.class);
        canUpdatePost(post, postId, userId);
        post.setTitle(addPostDto.getTitle());
        post.setContent(addPostDto.getContent());
        return (postRepository.save(post));
    }

    @Transactional
    public Page<Post> getForumPosts(ForumPostsRequestDTO forumPostsRequestDTO) {
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
    public void deletePost(ObjectId postId, Long userId) {
        if (!accessService.canDeletePost(longToObjectId(userId), postId)) {
            throw new AccessDeniedException("User " + " cannot delete this post");
        }
        Post post = mongoTemplate.findById(postId, Post.class);
        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }
        Forum forum = mongoTemplate.findById(post.getForumId(), Forum.class);
        forum.setPostCount(forum.getPostCount() - 1);
        forumRepository.save(forum);
        deletionService.deletePost(postId);
    }

    public Page<Post> getUserPosts(Long userId, MainFeedRequestDTO mainFeedRequestDTO) {
        Pageable pageable = PageRequest.of(
                mainFeedRequestDTO.getPage(),
                mainFeedRequestDTO.getPageSize());
        List<ObjectId> forumIds = followingRepository.findForumIdsByUserId(longToObjectId(userId)).stream()
                .map(d -> d.getObjectId("forumId"))
                .toList();
        ;
        return postRepository.findByIsDeletedFalseAndForumIdIn(forumIds, pageable);
    }

    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }

    public Post getPostById(ObjectId postId) {
        return mongoTemplate.findById(postId, Post.class);
    }
}
