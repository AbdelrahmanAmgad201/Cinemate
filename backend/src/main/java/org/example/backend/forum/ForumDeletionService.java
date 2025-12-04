package org.example.backend.forum;

import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.comment.CommentRepository;
import org.example.backend.post.PostRepository;
import org.example.backend.vote.VoteRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ForumDeletionService {
    private final ForumRepository forumRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;

    private static final int BATCH_SIZE = 100;

    public void deleteForum(ObjectId forumId) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new RuntimeException("Forum not found"));

        if (forum.getIsDeleted()) {
            throw new RuntimeException("Forum already deleted");
        }

        forum.setIsDeleted(true);
        forum.setDeletedAt(Instant.now());
        forumRepository.save(forum);

        cascadeDeleteForumAsync(forumId);
    }

    @Async
    public void cascadeDeleteForumAsync(ObjectId forumId){
        // background job to softly delete
        // hard delete every 30 days or so
        return;
    }
}
