package org.example.backend.hateSpeach;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.comment.CommentService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@EnableScheduling
@Service
@RequiredArgsConstructor
public class HateSpeachScheduler {

    private final CommentService commentService;
    private HateSpeachService hateSpeachService;
    private final CommentRepository commentRepository;
    @Scheduled(cron = "0 0 0 * * ?")
    public void schedule(){
        cleanHateSpeachComments();
    }

    private void cleanHateSpeachComments(){
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        List<Comment> todayComments = commentRepository.findAllByCreatedAtBetween(yesterday,now);
        for(Comment comment : todayComments){
            cleanComment(comment);
        }
    }
    private void cleanComment(Comment comment){
        if(comment.getIsDeleted()){
            return;
        }
        if(!hateSpeachService.analyzeText(comment.getContent())){
            commentService.systemDeleteComment(comment);
        }
    }

}
