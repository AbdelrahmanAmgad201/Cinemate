package org.example.backend.vote;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.el.parser.BooleanNode;
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

import java.util.List;

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
        Votable target = canVote(targetId,isPost);
        incrementVote(target,upVote);
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
        ObjectId objectUserId = longToObjectId(userId);
        ObjectId targetId = updateVoteDTO.getTargetId();
        Vote vote = voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(objectUserId,targetId);
        if(vote == null){
            throw new IllegalArgumentException("Vote not found");
        }
        if (!vote.getUserId().equals(longToObjectId(userId))) {
            throw new AccessDeniedException("User does not have permission to update this forum");
        }
        Boolean upVote = updateVoteDTO.getValue().equals(1);
        Votable target = canVote(vote.getTargetId(),vote.getIsPost());
        updateIncrement(target,upVote);
        vote.setVoteType(updateVoteDTO.getValue());
        voteRepository.save(vote);
    }

    @Transactional
    public Integer isVote(ObjectId targetId,Long userId) {
        ObjectId objectUserId = longToObjectId(userId);
        Vote vote = voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(objectUserId,targetId);
        if (vote==null) return 0;
        return vote.getVoteType();
    }

    @Transactional
    public void deleteVote(ObjectId targetId,Long userId){
        ObjectId objectUserId = longToObjectId(userId);
        Vote vote = voteRepository.findByIsDeletedIsFalseAndUserIdAndTargetId(objectUserId,targetId);
        if(vote==null){
            throw new IllegalArgumentException("Vote not found");
        }
        ObjectId voteId = vote.getId();
        if (!accessService.canDeleteVote(longToObjectId(userId), voteId)) {
            throw new AccessDeniedException("User " + " cannot delete this vote");
        }
        Votable target = canVote(vote.getTargetId(),vote.getIsPost());
        Boolean upVote = vote.getVoteType().equals(1);
        decrementVote(target,upVote);
        deletionService.deleteVote(voteId);
    }

    private Votable canVote(ObjectId targetId,Boolean isPost){
        if(isPost)  return canVotePost(targetId);
        else return canVoteComment(targetId);
    }

    private Post canVotePost(ObjectId postId){

        Post post = mongoTemplate.findById(postId, Post.class);

        if (post == null) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }

        if (post.getIsDeleted()) {
            throw new IllegalStateException("Cannot vote a deleted post");
        }
        return post;
    }

    private Comment canVoteComment(ObjectId commentId){
        Comment comment = mongoTemplate.findById(commentId, Comment.class);

        if (comment == null) {
            throw new IllegalArgumentException("Post not found with id: " + commentId);
        }

        if (comment.getIsDeleted()) {
            throw new IllegalStateException("Cannot vote a deleted comment");
        }
        return comment;
    }

    private void incrementVote(Votable target,Boolean upVote) {
        if(upVote) {
            target.incrementUpvote();
        }
        else {
            target.incrementDownvote();
        }
        if(target instanceof Post post) {
            postRepository.save(post);
        }
        if(target instanceof Comment comment) {
            commentRepository.save(comment);
        }
    }

    private void decrementVote(Votable target,Boolean upVote) {
        if(upVote) {
            target.decrementUpvote();
        }
        else {
            target.decrementDownvote();
        }
        if(target instanceof Post post) {
            postRepository.save(post);
        }
        if(target instanceof Comment comment) {
            commentRepository.save(comment);
        }
    }

    private void updateIncrement(Votable target,Boolean upVote){
        if(upVote) {
            target.incrementUpvote();
            target.decrementDownvote();
        }
        else {
            target.incrementDownvote();
            target.decrementUpvote();
        }
        if(target instanceof Post post) {
            postRepository.save(post);
        }
        if(target instanceof Comment comment) {
            commentRepository.save(comment);
        }
    }

    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}
