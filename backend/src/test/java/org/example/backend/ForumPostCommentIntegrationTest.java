package org.example.backend;

import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumRepository;
import org.example.backend.post.Post;
import org.example.backend.post.PostRepository;
import org.example.backend.comment.Comment;
import org.example.backend.comment.CommentRepository;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ForumPostCommentIntegrationTest {

    @Autowired
    private ForumRepository forumRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void fullFlow_createForum_addPosts_addComments() {

        // create forum
        Forum forum = Forum.builder()
                .id(new ObjectId())
                .name("Programming Forum")
                .description("A place to talk about code")
                .ownerId(new ObjectId())
                .createdAt(Instant.now())
                .build();

        forumRepository.save(forum);

        Forum savedForum = forumRepository.findById(forum.getId()).orElse(null);
        assertThat(savedForum).isNotNull();

        // add posts to forum
        Post post1 = Post.builder()
                .id(new ObjectId())
                .forumId(forum.getId())
                .ownerId(new ObjectId())
                .title("First Post")
                .content("Hello World")
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .build();

        Post post2 = Post.builder()
                .id(new ObjectId())
                .forumId(forum.getId())
                .ownerId(new ObjectId())
                .title("Second Post")
                .content("Spring Boot is amazing")
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .build();

        postRepository.save(post1);
        postRepository.save(post2);

        // check if saved correctly
        List<Post> forumPosts = postRepository.findByForumId(forum.getId());
        assertThat(forumPosts).hasSize(2);


        // ============================
        // 3. Add comments on post1
        // ============================

        Comment c1 = Comment.builder()
                .id(new ObjectId())
                .postId(post1.getId())
                .ownerId(new ObjectId())
                .content("Nice post!")
                .createdAt(Instant.now())
                .build();

        Comment c2 = Comment.builder()
                .id(new ObjectId())
                .postId(post1.getId())
                .parentId(c1.getId()) // Reply to c1
                .depth(1)
                .ownerId(new ObjectId())
                .content("Thanks!")
                .createdAt(Instant.now())
                .build();

        commentRepository.save(c1);
        commentRepository.save(c2);

        List<Comment> post1Comments = commentRepository.findAll();

        assertThat(post1Comments).hasSize(2);
        assertThat(post1Comments)
                .extracting(Comment::getContent)
                .containsExactlyInAnyOrder("Nice post!", "Thanks!");

        assertThat(post1Comments)
                .extracting(Comment::getPostId)
                .containsOnly(post1.getId());
    }
}
