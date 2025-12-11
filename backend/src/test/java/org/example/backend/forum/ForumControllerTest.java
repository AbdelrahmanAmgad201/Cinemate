package org.example.backend.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ForumController.class)
@WithMockUser
class ForumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ForumService forumService;

    private Long userId;
    private ObjectId forumId;
    private Forum testForum;
    private ForumCreationRequest creationRequest;

    @BeforeEach
    void setUp() {
        userId = 123456L;
        forumId = new ObjectId();

        testForum = Forum.builder()
                .id(forumId)
                .name("Test Forum")
                .description("Test Description")
                .followerCount(10)
                .postCount(5)
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();

        creationRequest = new ForumCreationRequest();
        creationRequest.setName("New Forum");
        creationRequest.setDescription("New Forum Description");
    }

    // ==================== CREATE FORUM TESTS ====================

    @Test
    void createForum_ValidRequest_Success() throws Exception {
        when(forumService.createForum(any(ForumCreationRequest.class), anyLong()))
                .thenReturn(testForum);

        mockMvc.perform(post("/api/forum/v1/create")
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Forum"))
                .andExpect(jsonPath("$.description").value("Test Description"));

        verify(forumService).createForum(argThat(req ->
                req.getName().equals("New Forum") &&
                        req.getDescription().equals("New Forum Description")
        ), eq(userId));
    }



    @Test
    void createForum_WithoutUserId_CallsServiceWithNull() throws Exception {
        when(forumService.createForum(any(ForumCreationRequest.class), isNull()))
                .thenReturn(testForum);

        mockMvc.perform(post("/api/forum/v1/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationRequest)))
                .andExpect(status().isOk());

        verify(forumService).createForum(any(ForumCreationRequest.class), isNull());
    }

    // ==================== DELETE FORUM TESTS ====================

    @Test
    void deleteForum_Success() throws Exception {
        doNothing().when(forumService).deleteForum(any(ObjectId.class), anyLong());

        mockMvc.perform(delete("/api/forum/v1/delete/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId))
                .andExpect(status().isOk());

        verify(forumService).deleteForum(eq(forumId), eq(userId));
    }

    @Test
    void deleteForum_Unauthorized_Returns500() throws Exception {
        doThrow(new AccessDeniedException("User cannot delete this forum"))
                .when(forumService).deleteForum(any(ObjectId.class), anyLong());

        mockMvc.perform(delete("/api/forum/v1/delete/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId))
                .andExpect(status().is5xxServerError());
    }


    @Test
    void updateForum_Success() throws Exception {
        when(forumService.updateForum(any(ObjectId.class), any(ForumCreationRequest.class), anyLong()))
                .thenReturn(testForum);

        mockMvc.perform(put("/api/forum/v1/update/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationRequest)))
                .andExpect(status().isOk());

        verify(forumService).updateForum(eq(forumId), argThat(req ->
                req.getName().equals("New Forum") &&
                        req.getDescription().equals("New Forum Description")
        ), eq(userId));
    }

    @Test
    void updateForum_NotOwner_Returns500() throws Exception {
        when(forumService.updateForum(any(ObjectId.class), any(ForumCreationRequest.class), anyLong()))
                .thenThrow(new AccessDeniedException("User does not have permission"));

        mockMvc.perform(put("/api/forum/v1/update/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void updateForum_DeletedForum_Returns500() throws Exception {
        when(forumService.updateForum(any(ObjectId.class), any(ForumCreationRequest.class), anyLong()))
                .thenThrow(new IllegalStateException("Cannot update a deleted forum"));

        mockMvc.perform(put("/api/forum/v1/update/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creationRequest)))
                .andExpect(status().is5xxServerError());
    }


    // ==================== SEARCH FORUMS TESTS ====================

    @Test
    void searchForums_WithResults_Success() throws Exception {
        Forum forum1 = Forum.builder()
                .id(new ObjectId())
                .name("JavaScript Forum")
                .description("JS discussions")
                .build();

        Forum forum2 = Forum.builder()
                .id(new ObjectId())
                .name("Java Forum")
                .description("Java discussions")
                .build();

        SearchResultDto searchResult = SearchResultDto.builder()
                .forums(Arrays.asList(forum1, forum2))
                .currentPage(0)
                .totalPages(1)
                .totalElements(2)
                .pageSize(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(forumService.searchForums(eq("java"), any(Pageable.class)))
                .thenReturn(searchResult);

        mockMvc.perform(get("/api/forum/v1/search")
                        .param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forums").isArray())
                .andExpect(jsonPath("$.forums.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(forumService).searchForums(eq("java"), any(Pageable.class));
    }

    @Test
    void searchForums_EmptyQuery_Returns400() throws Exception {
        mockMvc.perform(get("/api/forum/v1/search")
                        .param("q", ""))
                .andExpect(status().isBadRequest());

        verify(forumService, never()).searchForums(anyString(), any());
    }

    @Test
    void searchForums_WhitespaceQuery_Returns400() throws Exception {
        mockMvc.perform(get("/api/forum/v1/search")
                        .param("q", "   "))
                .andExpect(status().isBadRequest());

        verify(forumService, never()).searchForums(anyString(), any());
    }


    @Test
    void searchForums_WithPagination_Success() throws Exception {
        SearchResultDto searchResult = SearchResultDto.builder()
                .forums(List.of())
                .currentPage(2)
                .totalPages(5)
                .totalElements(50)
                .pageSize(10)
                .hasNext(true)
                .hasPrevious(true)
                .build();

        when(forumService.searchForums(eq("test"), any(Pageable.class)))
                .thenReturn(searchResult);

        mockMvc.perform(get("/api/forum/v1/search")
                        .param("q", "test")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(2))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    void searchForums_TrimsSearchQuery() throws Exception {
        SearchResultDto searchResult = SearchResultDto.builder()
                .forums(List.of())
                .currentPage(0)
                .totalPages(0)
                .totalElements(0)
                .pageSize(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(forumService.searchForums(eq("trimmed"), any(Pageable.class)))
                .thenReturn(searchResult);

        mockMvc.perform(get("/api/forum/v1/search")
                        .param("q", "  trimmed  "))
                .andExpect(status().isOk());

        verify(forumService).searchForums(eq("trimmed"), any(Pageable.class));
    }

    @Test
    void searchForums_DefaultPagination_Uses20PageSize() throws Exception {
        SearchResultDto searchResult = SearchResultDto.builder()
                .forums(List.of())
                .currentPage(0)
                .totalPages(0)
                .totalElements(0)
                .pageSize(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(forumService.searchForums(anyString(), any(Pageable.class)))
                .thenReturn(searchResult);

        mockMvc.perform(get("/api/forum/v1/search")
                        .param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(20));

        verify(forumService).searchForums(eq("test"), argThat(pageable ->
                pageable.getPageSize() == 20 && pageable.getPageNumber() == 0
        ));
    }

    @Test
    void searchForums_NoResults_ReturnsEmptyList() throws Exception {
        SearchResultDto searchResult = SearchResultDto.builder()
                .forums(List.of())
                .currentPage(0)
                .totalPages(0)
                .totalElements(0)
                .pageSize(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(forumService.searchForums(eq("nonexistent"), any(Pageable.class)))
                .thenReturn(searchResult);

        mockMvc.perform(get("/api/forum/v1/search")
                        .param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forums").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}