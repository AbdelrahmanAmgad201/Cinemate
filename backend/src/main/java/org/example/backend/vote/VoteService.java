package org.example.backend.vote;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import org.bson.types.ObjectId;
import org.example.backend.comment.Comment;
import org.example.backend.deletion.AccessService;
import org.example.backend.deletion.CascadeDeletionService;
import org.example.backend.post.Post;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import static org.example.backend.util.IdConverter.longToObjectId;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VoteService {
    private final VoteRepository voteRepository;
    private final MongoTemplate mongoTemplate;
    private final CascadeDeletionService deletionService;
    private final AccessService accessService;

    @Transactional
    public void vote(VoteDTO voteDTO,VoteTargetType targetType,Long userId) {
        ObjectId targetId = voteDTO.getTargetId();
        ObjectId ownerId = longToObjectId(userId);
        Boolean upVote = voteDTO.getValue().equals(1);
        Votable target = canVote(targetId,targetType);
        incrementVote(target,upVote);
        Vote vote = Vote.builder()
                .targetId(targetId)
                .userId(ownerId)
                .voteType(voteDTO.getValue())
                .targetType(targetType)
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
        Votable target = canVote(vote.getTargetId(),vote.getTargetType());
        updateIncrement(target,upVote);
        vote.setVoteType(updateVoteDTO.getValue());
        voteRepository.save(vote);
    }

    @Transactional(readOnly = true)
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
        Votable target = canVote(vote.getTargetId(),vote.getTargetType());
        Boolean upVote = vote.getVoteType().equals(1);
        decrementVote(target,upVote);
        deletionService.deleteVote(voteId);
    }

    private Votable canVote(ObjectId targetId,VoteTargetType targetType){
        if(targetType == VoteTargetType.POST)  return canVotePost(targetId);
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
            throw new IllegalArgumentException("Comment not found with id: " + commentId);
        }

        if (comment.getIsDeleted()) {
            throw new IllegalStateException("Cannot vote a deleted comment");
        }
        return comment;
    }

    // Applies vote-count changes atomically via MongoDB's $inc rather than
    // load-mutate-save, which loses concurrent updates under any real load
    // (REL-01: two simultaneous votes both read count=5, both write count=6).
    private void incrementVote(Votable target, Boolean upVote) {
        applyVoteDelta(target, upVote ? 1 : 0, upVote ? 0 : 1);
    }

    private void decrementVote(Votable target, Boolean upVote) {
        applyVoteDelta(target, upVote ? -1 : 0, upVote ? 0 : -1);
    }

    private void updateIncrement(Votable target, Boolean upVote) {
        applyVoteDelta(target, upVote ? 1 : -1, upVote ? -1 : 1);
    }

    private void applyVoteDelta(Votable target, int upvoteDelta, int downvoteDelta) {
        int scoreDelta = upvoteDelta - downvoteDelta;
        if (target instanceof Post post) {
            Update update = new Update()
                    .inc("upvoteCount", upvoteDelta)
                    .inc("downvoteCount", downvoteDelta)
                    .inc("score", scoreDelta)
                    .set("lastActivityAt", Instant.now());
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(post.getId())), update, Post.class);
        } else if (target instanceof Comment comment) {
            Update update = new Update()
                    .inc("upvoteCount", upvoteDelta)
                    .inc("downvoteCount", downvoteDelta)
                    .inc("score", scoreDelta);
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(comment.getId())), update, Comment.class);
        }
    }

}
