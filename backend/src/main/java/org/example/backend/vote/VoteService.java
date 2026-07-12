package org.example.backend.vote;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Voting over the two typed tables (post_votes, comment_votes). The DB triggers maintain
 * the target's upvote_count/downvote_count (score is a generated column), so this service
 * only writes vote rows. A user can only read/delete their own vote (queried by userId),
 * so no separate ownership check is needed. Because UUIDs are unique across posts and
 * comments, targetId alone identifies which table a vote lives in.
 */
@Service
@RequiredArgsConstructor
public class VoteService {

    private final PostVoteRepository postVoteRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void vote(VoteDTO voteDTO, VoteTargetType targetType, Long userId) {
        UUID targetId = voteDTO.getTargetId();
        if (targetType == VoteTargetType.POST) {
            requireActivePost(targetId);
            PostVote vote = postVoteRepository.findByUserIdAndPostId(userId, targetId)
                    .orElseGet(() -> PostVote.builder().userId(userId).postId(targetId).build());
            vote.setVoteType(voteDTO.getValue());
            postVoteRepository.save(vote);
        } else {
            requireActiveComment(targetId);
            CommentVote vote = commentVoteRepository.findByUserIdAndCommentId(userId, targetId)
                    .orElseGet(() -> CommentVote.builder().userId(userId).commentId(targetId).build());
            vote.setVoteType(voteDTO.getValue());
            commentVoteRepository.save(vote);
        }
    }

    @Transactional
    public void updateVote(UpdateVoteDTO updateVoteDTO, Long userId) {
        UUID targetId = updateVoteDTO.getTargetId();
        PostVote pv = postVoteRepository.findByUserIdAndPostId(userId, targetId).orElse(null);
        if (pv != null) {
            requireActivePost(targetId);
            pv.setVoteType(updateVoteDTO.getValue());
            postVoteRepository.save(pv);
            return;
        }
        CommentVote cv = commentVoteRepository.findByUserIdAndCommentId(userId, targetId).orElse(null);
        if (cv != null) {
            requireActiveComment(targetId);
            cv.setVoteType(updateVoteDTO.getValue());
            commentVoteRepository.save(cv);
            return;
        }
        throw new IllegalArgumentException("Vote not found");
    }

    @Transactional(readOnly = true)
    public Integer isVote(UUID targetId, Long userId) {
        return postVoteRepository.findByUserIdAndPostId(userId, targetId).map(PostVote::getVoteType)
                .or(() -> commentVoteRepository.findByUserIdAndCommentId(userId, targetId).map(CommentVote::getVoteType))
                .orElse(0);
    }

    @Transactional
    public void deleteVote(UUID targetId, Long userId) {
        PostVote pv = postVoteRepository.findByUserIdAndPostId(userId, targetId).orElse(null);
        if (pv != null) {
            postVoteRepository.delete(pv);
            return;
        }
        CommentVote cv = commentVoteRepository.findByUserIdAndCommentId(userId, targetId).orElse(null);
        if (cv != null) {
            commentVoteRepository.delete(cv);
            return;
        }
        throw new IllegalArgumentException("Vote not found");
    }

    private void requireActivePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
        if (Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new IllegalStateException("Cannot vote a deleted post");
        }
    }

    private void requireActiveComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + commentId));
        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new IllegalStateException("Cannot vote a deleted comment");
        }
    }
}
