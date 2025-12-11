package org.example.backend.post;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PostService {
    private final RestTemplate restTemplate;
    private final ForumRepository forumRepository;

    @Value("${hatespeech.model.url}")
    private String url;
    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;

    @Transactional
    public Post addPost(AddPostDto addPostDto, Long userId) {
        if (!analyzeText(addPostDto.getContent())) {
            throw new HateSpeechException("hate speech detected");
        }
        ObjectId ObjectUserId = longToObjectId(userId);
        Forum forum = mongoTemplate.findById(addPostDto.getForumId(), Forum.class);
        forum.setPostCount(forum.getPostCount() + 1);
        forumRepository.save(forum);
        Post post = Post.builder()
                .ownerId(ObjectUserId)
                .forumId(addPostDto.getForumId())
                .title(addPostDto.getTitle())
                .content(addPostDto.getContent())
                .build();
        return (postRepository.save(post));
    }

    @Transactional
    public Post updatePost(ObjectId postId,AddPostDto addPostDto, Long userId){
        if (!analyzeText(addPostDto.getContent())) {
            throw new HateSpeechException("hate speech detected");
        }
        Post post = mongoTemplate.findById(postId, Post.class);
        canUpdatePost(post,postId,userId);
        post.setTitle(addPostDto.getTitle());
        post.setContent(addPostDto.getContent());
        return (postRepository.save(post));
    }

    @Transactional
    public Page<Post> getForumPosts(ForumPostsRequestDTO forumPostsRequestDTO) {
        Pageable pageable = PageRequest.of(
                forumPostsRequestDTO.getPage(),
                forumPostsRequestDTO.getPageSize()
        );
        return  postRepository.findByForumId(forumPostsRequestDTO.getForumId(), pageable);
    }

    private void canUpdatePost(Post post,ObjectId postId, Long userId){
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

    public boolean analyzeText(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Safe JSON string
        String body = "{\"text\":\"" + text.replace("\"", "\\\"") + "\"}";

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Boolean> response =
                restTemplate.postForEntity(url, request, Boolean.class);
        return response.getBody();
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
        forum.setPostCount(forum.getPostCount() + 1);
        forumRepository.save(forum);
        deletionService.deletePost(postId);
    }

    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}
