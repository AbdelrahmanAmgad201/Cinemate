package org.example.backend.post;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class PostService {
    private final RestTemplate restTemplate = new RestTemplate();

    private final String url = "http://localhost:8000/api/hate/v1/analyze";
    private final PostRepository postRepository;
    private static final Logger log = LoggerFactory.getLogger(PostService.class);


    public void addPost(AddPostDto addPostDto, Long userId) {
        if (!analyzeText(addPostDto.getContent())) {
            throw new RuntimeException("There is hate speech");
        }
        ObjectId ObjectUserId = longToObjectId(userId);
        Post post = Post.builder()
                .ownerId(ObjectUserId)
                .forumId(addPostDto.getForumId())
                .title(addPostDto.getTitle())
                .content(addPostDto.getContent())
                .build();
        postRepository.save(post);
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
