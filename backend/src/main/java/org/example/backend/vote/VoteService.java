package org.example.backend.vote;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoteService {
    @Autowired
    private VoteRepository voteRepository;

    private final MongoTemplate mongoTemplate;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;

    @Transactional
    public void vote(VoteDTO voteDTO,Boolean isPost,Long userId) {
        ObjectId targetId = voteDTO.getTargetId();
        ObjectId ownerId = longToObjectId(userId);
        Boolean upVote = voteDTO.getValue().equals(1);
        canVote(targetId,isPost,upVote);
        Vote vote = Vote.builder()
                .targetId(targetId)
                .userId(ownerId)
                .voteType(voteDTO.getValue())
                .isPost(isPost)
                .build();
        voteRepository.save(vote);
    }

    @Transactional
    public void updateVote(UpdateVoteDTO updateVoteDTO,Long userId) {
        ObjectId voteId = updateVoteDTO.getId();
        Vote vote = mongoTemplate.findById(voteId, Vote.class);
        if(vote == null){
            throw new IllegalArgumentException("Vote not found with id: " + voteId);
        }
        if (!vote.getUserId().equals(longToObjectId(userId))) {
            throw new AccessDeniedException("User does not have permission to update this forum");
        }
        vote.setVoteType(updateVoteDTO.getValue());
        voteRepository.save(vote);
    }

    @Transactional
    public void deleteVote(ObjectId voteId,Long userId){
        if (!accessService.canDeleteVote(longToObjectId(userId), voteId)) {
            throw new AccessDeniedException("User " + " cannot delete this vote");
        }

        deletionService.deleteVote(voteId);
    }

    private void canVote(ObjectId targetId,Boolean isPost,Boolean upVote){
        if(isPost)  canVotePost(targetId,upVote);
        else canVoteComment(targetId,upVote);
    }

    private void canVotePost(ObjectId postId,Boolean upVote){

        Post post = mongoTemplate.findById(postId, Post.class);

        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }

        if (post.getIsDeleted()) {
            throw new IllegalStateException("Cannot vote a deleted post");
        }
        if (upVote) {
            post.setUpvoteCount(post.getUpvoteCount() + 1);
        }
        else  {
            post.setUpvoteCount(post.getDownvoteCount() + 1);
        }
        postRepository.save(post);
    }

    private void canVoteComment(ObjectId commentId,Boolean upVote){
        Comment comment = mongoTemplate.findById(commentId, Comment.class);

        if (comment == null) {
            throw new IllegalArgumentException("Post not found with id: " + commentId);
        }

        if (comment.getIsDeleted()) {
            throw new IllegalStateException("Cannot vote a deleted comment");
        }
        if (upVote) {
            comment.setUpvoteCount(comment.getUpvoteCount() + 1);
        }
        else  {
            comment.setUpvoteCount(comment.getDownvoteCount() + 1);
        }
        commentRepository.save(comment);
    }
    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}
