package org.example.backend.dataInitializer;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.bcel.Const;
import org.example.backend.admin.AddAdminDTO;
import org.example.backend.admin.Admin;
import org.example.backend.admin.AdminService;
import org.example.backend.comment.AddCommentDTO;
import org.example.backend.comment.CommentService;
import org.example.backend.forum.Forum;
import org.example.backend.forum.ForumCreationRequest;
import org.example.backend.forum.ForumService;
import org.example.backend.forumfollowing.FollowingService;
import org.example.backend.movie.*;
import org.example.backend.organization.Organization;
import org.example.backend.organization.OrganizationService;
import org.example.backend.post.AddPostDto;
import org.example.backend.post.Post;
import org.example.backend.post.PostService;
import org.example.backend.user.User;
import org.example.backend.user.UserDataDTO;
import org.example.backend.user.UserService;
import org.example.backend.vote.VoteDTO;
import org.example.backend.vote.VoteService;
import org.example.backend.watchHistory.WatchHistory;
import org.example.backend.watchHistory.WatchHistoryService;
import org.example.backend.watchLater.WatchLaterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.file.WatchService;
import java.time.LocalDate;

import static org.example.backend.movie.Genre.ANIMATION;


@Component
@Profile("dev") // IMPORTANT: only runs in dev profile
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final String defaultPass = "12345678";
    private final int defaultNumberOfUsers = 100;
    private final int defaultNumberOfPostsPerUser = 10;
    private final int defaultNumberOfVotesPerPost = 10;     //make sure it is smaller than number of users
    private final int defaultNumberOfCommentsPerPost = 10;  //make sure it is smaller than number of users
    private final int defaultNumberOfFollowingPosts = 10;   //make sure it is smaller than number of users
    private final int defaultNumberOfOrganizations=10;
    private final int defaultNumberOfMoviesPerOrganization=5;

    private final UserService userService;
    private final CommentService commentService;
    private final ForumService forumService;
    private final FollowingService followingService;
    private final PostService postService;
    private final VoteService voteService;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;
    private final OrganizationService organizationService;
    private final WatchHistoryService watchHistoryService;
    private final WatchLaterService watchLaterService;
    private final MovieService movieService;
    private final MovieRepository movieRepository;


    @Value("${app.data-init.enabled:false}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        String hashedPassword = passwordEncoder.encode(defaultPass);
        for(int i=1;i<=defaultNumberOfUsers;i++){
            User user=initializeUser(i,hashedPassword);
            Forum forum=initializeForum(i,user);
            forumFollowing(forum);
            for(int j=1;j<=defaultNumberOfPostsPerUser;j++){
                initializePostWithVotesAndComments(i,j,forum,user);
            }
        }
        AddAdminDTO addAdminDTO = new AddAdminDTO();
        addAdminDTO.setPassword(defaultPass);
        addAdminDTO.setName("testAdmin");
        addAdminDTO.setEmail("testAdmin@example.com");
        Admin admin= adminService.addAdmin(addAdminDTO);
        for(int i=1;i<=defaultNumberOfOrganizations;i++){
            initializeOrganization(Long.valueOf(i),admin);
        }
    }

    private User initializeUser(int i,String hashedPassword){
        User user=userService.addUser("testUser"+i+"@example.com",hashedPassword);
        UserDataDTO userDataDTO = UserDataDTO.builder()
                .firstName("tester")
                .lastName("n"+i)
                .gender("male")
                .about("only for test")
                .birthday(LocalDate.now())
                .build();
        userService.setUserData(user.getId(), userDataDTO);
        return user;
    }

    private Forum initializeForum(int i,User user){
        ForumCreationRequest forumCreationRequest= ForumCreationRequest.builder()
                .name("test"+i)
                .description("the forum for user"+i)
                .build();
        Forum forum=forumService.createForum(forumCreationRequest,user.getId());
        return forum;
    }

    private void forumFollowing(Forum forum){
        for(int j=1;j<=defaultNumberOfFollowingPosts;j++){
            followingService.follow(forum.getId(),Long.valueOf(j));
        }
    }

    private void initializePostWithVotesAndComments(int forumNumber,int postNumber,Forum forum,User user){
        AddPostDto addPostDto = AddPostDto.builder()
                .forumId(forum.getId())
                .title("test"+postNumber+"for forum"+forumNumber)
                .content("this only for test")
                .build();
        Post post = postService.addPost(addPostDto,user.getId());
        initializeVotes(post);
        initializeComments(postNumber,forumNumber,post);
    }

    private void initializeVotes(Post post){
        for (int k=1;k<=defaultNumberOfVotesPerPost;k++){
            VoteDTO voteDTO = VoteDTO.builder()
                    .targetId(post.getId())
                    .value(1)
                    .build();
            voteService.vote(voteDTO,true,Long.valueOf(k));
        }
    }

    private void initializeComments(int postNumber,int forumNumber,Post post){
        for (int k=1;k<=defaultNumberOfCommentsPerPost;k++){
            AddCommentDTO addCommentDTO = AddCommentDTO.builder()
                    .postId(post.getId())
                    .content("comment "+k+" on post "+postNumber+" on user "+forumNumber)
                    .build();
            commentService.addComment(Long.valueOf(k),addCommentDTO);
        }
    }

    private void initializeOrganization(Long orgId, Admin admin){
        String email = "testOrg"+orgId+"@example.com";
        String hashedPassword = passwordEncoder.encode(defaultPass);
        Organization organization=organizationService.addOrganization(email,hashedPassword);
        for (int i =1 ;i<=defaultNumberOfMoviesPerOrganization;i++){
            Movie movie = Movie.builder()
                    .organization(organization)
                    .name("movie "+i+ " for org "+orgId)
                    .description( "movie only for test")
                    .thumbnailUrl("https://th.bing.com/th/id/R.29c4ad8d766033e15f7a9e8e8ec0e204?rik=i10dQJcgOoPmyw&pid=ImgRaw&r=0")
                    .duration(74)
                    .movieUrl("f6hqeoz7kv")
                    .trailerUrl("0gh0jqtgb0")
                    .genre(ANIMATION)
                    .admin(admin)
                    .build();
            movieRepository.save(movie);
            }
        }

}
