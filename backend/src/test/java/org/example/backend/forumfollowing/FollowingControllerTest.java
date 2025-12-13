package org.example.backend.forumfollowing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.example.backend.forum.Forum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FollowingController.class)
@WithMockUser
@ActiveProfiles("test")
class FollowingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FollowingService followingService;

    private Long userId;
    private ObjectId forumId;

    @BeforeEach
    void setUp() {
        userId = 123456L;
        forumId = new ObjectId();
    }

    @Test
    void followForum_Success() throws Exception {
        doNothing().when(followingService).follow(any(ObjectId.class), anyLong());

        mockMvc.perform(put("/api/forum-follow/v1/follow/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(followingService).follow(eq(forumId), eq(userId));
    }


    @Test
    void followForum_ServiceThrowsIllegalStateException_Returns500() throws Exception {
        doThrow(new IllegalStateException("Cannot follow deleted forum"))
                .when(followingService).follow(any(ObjectId.class), anyLong());

        mockMvc.perform(put("/api/forum-follow/v1/follow/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void unfollowForum_Success() throws Exception {
        doNothing().when(followingService).unfollow(any(ObjectId.class), anyLong());

        mockMvc.perform(delete("/api/forum-follow/v1/follow/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(followingService).unfollow(eq(forumId), eq(userId));
    }

    @Test
    void unfollowForum_ServiceThrowsException_Returns500() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(followingService).unfollow(any(ObjectId.class), anyLong());

        mockMvc.perform(delete("/api/forum-follow/v1/follow/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .requestAttr("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getFollowedForums_DefaultPagination_Success() throws Exception {
        Forum forum1 = new Forum();
        forum1.setId(new ObjectId());
        forum1.setName("Forum 1");

        Forum forum2 = new Forum();
        forum2.setId(new ObjectId());
        forum2.setName("Forum 2");

        ForumPageResponse response = ForumPageResponse.builder()
                .forums(Arrays.asList(forum1, forum2))
                .currentPage(0)
                .totalPages(1)
                .totalElements(2)
                .pageSize(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(followingService.getFollowedForums(eq(userId), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/forum-follow/v1/followed")
                        .with(csrf())
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forums").isArray())
                .andExpect(jsonPath("$.forums.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(followingService).getFollowedForums(eq(userId), argThat(pageable ->
                pageable.getPageNumber() == 0 &&
                        pageable.getPageSize() == 20 &&
                        pageable.getSort().getOrderFor("createdAt") != null &&
                        pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
        ));
    }

    @Test
    void getFollowedForums_CustomPagination_Success() throws Exception {
        ForumPageResponse response = ForumPageResponse.builder()
                .forums(List.of())
                .currentPage(2)
                .totalPages(5)
                .totalElements(50)
                .pageSize(10)
                .hasNext(true)
                .hasPrevious(true)
                .build();

        when(followingService.getFollowedForums(eq(userId), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/forum-follow/v1/followed")
                        .with(csrf())
                        .param("page", "2")
                        .param("size", "10")
                        .param("sort", "createdAt,asc")
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(2))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(followingService).getFollowedForums(eq(userId), argThat(pageable ->
                pageable.getPageNumber() == 2 &&
                        pageable.getPageSize() == 10
        ));
    }

    @Test
    void getFollowedForums_EmptyResults_Success() throws Exception {
        ForumPageResponse response = ForumPageResponse.builder()
                .forums(List.of())
                .currentPage(0)
                .totalPages(0)
                .totalElements(0)
                .pageSize(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(followingService.getFollowedForums(eq(userId), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/forum-follow/v1/followed")
                        .with(csrf())
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forums").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }



    @Test
    void followForum_WithoutUserId_StillCalls() throws Exception {
        // Test when userId is not set in request attributes
        doNothing().when(followingService).follow(any(ObjectId.class), isNull());

        mockMvc.perform(put("/api/forum-follow/v1/follow/{forumId}", forumId.toHexString())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(followingService).follow(eq(forumId), isNull());
    }
}