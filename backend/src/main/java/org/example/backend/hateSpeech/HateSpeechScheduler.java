package org.example.backend.hateSpeech;

import lombok.RequiredArgsConstructor;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;
import org.example.backend.comment.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@EnableScheduling
@Service
@RequiredArgsConstructor
public class HateSpeechScheduler {

    // PERF-04: process in fixed-size pages instead of loading every comment from the
    // last 24h into heap at once — at 50k+ comments/day this could otherwise load tens
    // of MB of JSON in one shot against a -Xmx512m heap.
    private static final int BATCH_SIZE = 100;

    private final CommentService commentService;
    private final HateSpeechService hateSpeechService;
    private final CommentRepository commentRepository;
    @Scheduled(cron = "0 0 0 * * ?")
    public void schedule(){
        cleanHateSpeachComments();
    }

    private void cleanHateSpeachComments(){
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        Page<Comment> page = commentRepository.findAllByCreatedAtBetween(yesterday, now, pageable);
        while (true) {
            page.getContent().forEach(this::cleanComment);
            if (!page.hasNext()) {
                break;
            }
            page = commentRepository.findAllByCreatedAtBetween(yesterday, now, page.nextPageable());
        }
    }
    private void cleanComment(Comment comment){
        if(comment.getIsDeleted()){
            return;
        }
        if(!hateSpeechService.analyzeText(comment.getContent())){
            commentService.systemDeleteComment(comment);
        }
    }

}
