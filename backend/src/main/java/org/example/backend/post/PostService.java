package org.example.backend.post;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PostService {
    private final RestTemplate restTemplate;

    @Value("${hatespeech.model.url}")
    private String url;
    private final PostRepository postRepository;
    private final MongoTemplate mongoTemplate;

    @Transactional
    public Post addPost(AddPostDto addPostDto, Long userId) {
        if (!analyzeText(addPostDto.getContent())) {
            throw new HateSpeechException("hate speech detected");
        }
        ObjectId ObjectUserId = longToObjectId(userId);
        Post post = Post.builder()
                .ownerId(ObjectUserId)
                .forumId(addPostDto.getForumId())
                .title(addPostDto.getTitle())
                .content(addPostDto.getContent())
                .build();
        return (postRepository.save(post));
    }

    @Transactional
    public Post updatePost(String postId,AddPostDto addPostDto, Long userId){
        if (!analyzeText(addPostDto.getContent())) {
            throw new HateSpeechException("hate speech detected");
        }
        ObjectId objectPostId = new ObjectId(postId);
        Post post = mongoTemplate.findById(postId, Post.class);
        canUpdatePost(post,objectPostId,userId);
        post.setTitle(addPostDto.getTitle());
        post.setContent(addPostDto.getContent());
        return (postRepository.save(post));
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
    
    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}
