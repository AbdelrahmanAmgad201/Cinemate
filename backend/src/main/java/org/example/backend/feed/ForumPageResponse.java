package org.example.backend.feed;

import lombok.Builder;
import org.example.backend.forum.Forum;
import org.example.backend.post.Post;

import java.util.List;

@Builder
public class ForumPageResponse {
    private List<Forum> forums;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
