package org.example.backend.post;

import org.example.backend.AbstractMongoIntegrationTest;
import org.bson.types.ObjectId;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PostServiceTest extends AbstractMongoIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumRepository forumRepository;

    @MockBean
    private RestTemplate restTemplate; // mock external AI API

    @MockBean
    private AccessService accessService; // for deletePost

    @MockBean
    private CascadeDeletionService deletionService; // for deletePost

    private final String url = "http://localhost:8000/api/hate/v1/analyze";

    // ---------------------------
    // addPost tests
    // ---------------------------
    @Test
    void testAddPost_whenCleanText_shouldSavePost() {
        ObjectId forumId = new ObjectId("00000000000000000000006f");

        // --- FIX: Save forum in DB ---
        Forum forum = Forum.builder()
                .id(forumId)
                .name("Test Forum")
                .postCount(0)
                .description("Test Description")
                .build();
        forumRepository.save(forum);
        // -----------------------------

        AddPostDto dto = new AddPostDto(forumId, "Test Title", "Normal content");
        Long userId = 5L;

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        Post savedPost = postService.addPost(dto, userId);

        Post fromDb = postRepository.findById(savedPost.getId()).orElse(null);
        assertThat(fromDb).isNotNull();
        assertThat(fromDb.getTitle()).isEqualTo("Test Title");
        assertThat(fromDb.getContent()).isEqualTo("Normal content");
        assertThat(fromDb.getForumId()).isEqualTo(forumId);
        assertThat(fromDb.getOwnerId()).isEqualTo(new ObjectId(String.format("%024x", userId)));

        verify(restTemplate, times(1))
                .postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class));
    }


    @Test
    void testAddPost_whenHateSpeech_shouldThrowException() {
        AddPostDto dto = new AddPostDto(new ObjectId("00000000000000000000006f"),
                "Bad Title", "Some hateful text");
        Long userId = 5L;

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(false, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        assertThatThrownBy(() -> postService.addPost(dto, userId))
                .isInstanceOf(HateSpeechException.class)
                .hasMessageContaining("hate speech detected");

        assertThat(postRepository.count()).isZero();
    }

    @Test
    void testAnalyzeText_shouldSendCorrectJsonToAi() {
        String text = "hello \"world\"";

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        boolean result = postService.analyzeText(text);

        assertThat(result).isTrue();

        verify(restTemplate).postForEntity(eq(url), captor.capture(), eq(Boolean.class));
        HttpEntity captured = captor.getValue();
        String jsonSent = (String) captured.getBody();
        assertThat(jsonSent).isEqualTo("{\"text\":\"hello \\\"world\\\"\"}");
        assertThat(captured.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    // ---------------------------
    // updatePost tests
    // ---------------------------
    @Test
    void testUpdatePost_whenCleanText_shouldUpdatePost() {
        Long userId = 7L;
        ObjectId postId = new ObjectId();
        Post existingPost = Post.builder()
                .id(postId)
                .ownerId(new ObjectId(String.format("%024x", userId)))
                .title("Old Title")
                .content("Old Content")
                .isDeleted(false)
                .build();
        postRepository.save(existingPost);

        AddPostDto dto = new AddPostDto(null, "New Title", "New Content");

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(true, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        Post updated = postService.updatePost(postId, dto, userId);

        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getContent()).isEqualTo("New Content");

        Post fromDb = postRepository.findById(postId).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("New Title");
        assertThat(fromDb.getContent()).isEqualTo("New Content");
    }

    @Test
    void testUpdatePost_whenHateSpeech_shouldThrowException() {
        Long userId = 7L;
        ObjectId postId = new ObjectId();
        Post existingPost = Post.builder()
                .id(postId)
                .ownerId(new ObjectId(String.format("%024x", userId)))
                .title("Old Title")
                .content("Old Content")
                .isDeleted(false)
                .build();
        postRepository.save(existingPost);

        AddPostDto dto = new AddPostDto(null, "Bad Title", "Hateful Content");

        ResponseEntity<Boolean> aiResponse = new ResponseEntity<>(false, HttpStatus.OK);
        when(restTemplate.postForEntity(eq(url), any(HttpEntity.class), eq(Boolean.class)))
                .thenReturn(aiResponse);

        assertThatThrownBy(() -> postService.updatePost(postId, dto, userId))
                .isInstanceOf(HateSpeechException.class)
                .hasMessageContaining("hate speech detected");

        Post fromDb = postRepository.findById(postId).orElseThrow();
        assertThat(fromDb.getTitle()).isEqualTo("Old Title");
        assertThat(fromDb.getContent()).isEqualTo("Old Content");
    }

    // ---------------------------
    // deletePost tests
    // ---------------------------
    @Test
    void testDeletePost_success() {
        Long userId = 9L;
        ObjectId postId = new ObjectId("6939b98be4433966bc84987d"); // manually generate an ObjectId

        ObjectId forumId = new ObjectId("00000000000000000000006f");

        // --- FIX: Save forum in DB ---
        Forum forum = Forum.builder()
                .id(forumId)
                .name("Test Forum")
                .postCount(1)
                .description("Test Description")
                .build();
        forumRepository.save(forum);

        // Create Post using builder and set the id explicitly
        Post post = Post.builder()
                .id(postId)  // assign id yourself
                .forumId(forumId)
                .ownerId(new ObjectId(String.format("%024x", userId)))
                .title("Title")
                .content("Content")
                .build();

        // Save post — MongoDB will accept the manually set id
        postRepository.save(post);

        // Mock dependent services
        when(accessService.canDeletePost(new ObjectId(String.format("%024x", userId)), postId))
                .thenReturn(true);
        doNothing().when(deletionService).deletePost(postId);

        // Call service
        postService.deletePost(postId, userId);

        // Verify
        verify(accessService, times(1))
                .canDeletePost(new ObjectId(String.format("%024x", userId)), postId);
        verify(deletionService, times(1)).deletePost(postId);
    }



    @Test
    void testDeletePost_accessDenied() {
        Long userId = 9L;
        ObjectId postId = new ObjectId();

        when(accessService.canDeletePost(new ObjectId(String.format("%024x", userId)), postId))
                .thenReturn(false);

        assertThatThrownBy(() -> postService.deletePost(postId, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot delete this post");

        verify(deletionService, never()).deletePost(any());
    }

    @Test
    void testDeletePost_runtimeException() {
        Long userId = 9L;
        ObjectId postId = new ObjectId();
        ObjectId forumId = new ObjectId("00000000000000000000006f");

        // Save a forum
        Forum forum = Forum.builder()
                .id(forumId)
                .name("Test Forum")
                .postCount(0)
                .description("Test Description")
                .build();
        forumRepository.save(forum);

        // Save a post so deletePost finds it
        Post post = Post.builder()
                .id(postId)
                .forumId(forumId)
                .ownerId(new ObjectId(String.format("%024x", userId)))
                .title("Title")
                .content("Content")
                .build();
        postRepository.save(post);

        // Mock accessService to allow deletion
        when(accessService.canDeletePost(new ObjectId(String.format("%024x", userId)), postId))
                .thenReturn(true);

        // Force deletionService to throw RuntimeException
        doThrow(new RuntimeException("failure")).when(deletionService).deletePost(postId);

        // Test that deletePost throws RuntimeException with message "failure"
        assertThatThrownBy(() -> postService.deletePost(postId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("failure");
    }


    // ---------------------------
    // get sorted posts (forum's)
    // ---------------------------

    @Test
    void testGetForumPosts_defaultSort_newestFirst() {
            ObjectId forumId = new ObjectId();

            // save forum
            Forum forum = Forum.builder().id(forumId).name("F").postCount(0).description("d").build();
            forumRepository.save(forum);

            // create posts with different createdAt
            Instant now = Instant.now();
            Post pOld = Post.builder().id(new ObjectId()).forumId(forumId).title("old").createdAt(now.minusSeconds(200)).score(1).build();
            Post pMid = Post.builder().id(new ObjectId()).forumId(forumId).title("mid").createdAt(now.minusSeconds(100)).score(2).build();
            Post pNew = Post.builder().id(new ObjectId()).forumId(forumId).title("new").createdAt(now).score(3).build();

            postRepository.save(pOld);
            postRepository.save(pMid);
            postRepository.save(pNew);

            ForumPostsRequestDTO dto = new ForumPostsRequestDTO();
            dto.setPage(0);
            dto.setPageSize(10);
            dto.setForumId(forumId);

            Page<Post> page = postService.getForumPosts(dto);

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getContent().get(0).getTitle()).isEqualTo("new");
            assertThat(page.getContent().get(1).getTitle()).isEqualTo("mid");
            assertThat(page.getContent().get(2).getTitle()).isEqualTo("old");
    }

    @Test
    void testGetForumPosts_oldSort_oldestFirst() {
            ObjectId forumId = new ObjectId();

            Forum forum = Forum.builder().id(forumId).name("F").postCount(0).description("d").build();
            forumRepository.save(forum);

            Instant now = Instant.now();
            Post pOld = Post.builder().id(new ObjectId()).forumId(forumId).title("old").createdAt(now.minusSeconds(200)).score(1).build();
            Post pMid = Post.builder().id(new ObjectId()).forumId(forumId).title("mid").createdAt(now.minusSeconds(100)).score(2).build();
            Post pNew = Post.builder().id(new ObjectId()).forumId(forumId).title("new").createdAt(now).score(3).build();

            postRepository.save(pOld);
            postRepository.save(pMid);
            postRepository.save(pNew);

            ForumPostsRequestDTO dto = new ForumPostsRequestDTO();
            dto.setPage(0);
            dto.setPageSize(10);
            dto.setForumId(forumId);
            dto.setSortBy("old");

            Page<Post> page = postService.getForumPosts(dto);

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getContent().get(0).getTitle()).isEqualTo("old");
            assertThat(page.getContent().get(1).getTitle()).isEqualTo("mid");
            assertThat(page.getContent().get(2).getTitle()).isEqualTo("new");
    }

    @Test
    void testGetForumPosts_topSort_scoreDesc_and_idDescTieBreak() {
            ObjectId forumId = new ObjectId();

            Forum forum = Forum.builder().id(forumId).name("F").postCount(0).description("d").build();
            forumRepository.save(forum);

            // p1 and p2 have same score; p2 should come before p1 because id is larger (id desc tie-break)
            Post p1 = Post.builder().id(new ObjectId("000000000000000000000001")).forumId(forumId).title("p1").score(10).createdAt(Instant.now()).build();
            Post p2 = Post.builder().id(new ObjectId("000000000000000000000002")).forumId(forumId).title("p2").score(10).createdAt(Instant.now()).build();
            Post p3 = Post.builder().id(new ObjectId("000000000000000000000003")).forumId(forumId).title("p3").score(5).createdAt(Instant.now()).build();

            postRepository.save(p1);
            postRepository.save(p2);
            postRepository.save(p3);

            ForumPostsRequestDTO dto = new ForumPostsRequestDTO();
            dto.setPage(0);
            dto.setPageSize(10);
            dto.setForumId(forumId);
            dto.setSortBy("top");

            Page<Post> page = postService.getForumPosts(dto);

            assertThat(page.getContent()).hasSize(3);
            // Expect p2 (id 2) first, then p1 (id 1), then p3
            assertThat(page.getContent().get(0).getTitle()).isEqualTo("p2");
            assertThat(page.getContent().get(1).getTitle()).isEqualTo("p1");
            assertThat(page.getContent().get(2).getTitle()).isEqualTo("p3");
    }


}
