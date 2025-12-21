package org.example.backend.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.example.backend.verification.Verfication;
import org.example.backend.verification.VerificationService;
import lombok.RequiredArgsConstructor;
import org.example.backend.security.CredentialsRequest;
import org.example.backend.security.JWTProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final Random random = new Random();
    @Autowired
    private VerificationService verificationService;
    @Autowired
    private JWTProvider jwtProvider;

    private final MongoTemplate mongoTemplate;
    private static final int BATCH_SIZE = 100;

    public User addUser(String email, String password) {
        User user = User.builder()
                .email(email)
                .password(password)
                .build();
       return userRepository.save(user);
    }


    @Transactional
    public String setUserData(Long userId, UserDataDTO userDataDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(userDataDTO.getFirstName());
        user.setLastName(userDataDTO.getLastName());
        user.setAbout(userDataDTO.getAbout());
        user.setBirthDate(userDataDTO.getBirthday());

        if (userDataDTO.getGender() != null) {
            user.setGender(Gender.valueOf(userDataDTO.getGender().toUpperCase()));
        }

        userRepository.save(user);

        return "User data updated successfully";
    }

    @Transactional
    public String completeProfile(Long userId, ProfileCompletionDTO profileData){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBirthDate(profileData.getBirthday());
        try{
            user.setGender(Gender.valueOf(profileData.getGender().toUpperCase()));
        } 
        catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Invalid gender value");
        }
        user.setProfileComplete(true);

        userRepository.save(user);

        return jwtProvider.generateToken(user);

    }

    @Transactional
    public Verfication signUp(CredentialsRequest credentialsRequest) {
        String email = credentialsRequest.getEmail();
        String password = credentialsRequest.getPassword();
        String role = credentialsRequest.getRole();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            throw new UserAlreadyExistsException(email);
        }
        int code = 100000 + random.nextInt(900000);
        if(verificationService.sendVerificationEmail(email, code)){
            return verificationService.addVerfication(email, password,code,role);
        }
        else{
            return new Verfication();
        }
    }

    @Transactional
    public void updateAbout(Long userId, String about) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAbout(about);
        userRepository.save(user);
    }

    @Transactional
    public void updateBirthDate(Long userId, LocalDate birthdate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBirthDate(birthdate);
        userRepository.save(user);
    }

    public void updateName(Long userId, UserName userName) {
        String firstName = userName.getFirstName();
        String lastName = userName.getLastName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);
        updatePostsUserName(user);
    }

    @Async
    protected void updatePostsUserName(User user) {
        ObjectId userId = longToObjectId(user.getId());
        String newName = user.getFirstName()+ " " + user.getLastName();
        try {
            // Get all post IDs for this user
            List<ObjectId> postIds = getIds("posts", Criteria.where("ownerId").is(userId));

            if (postIds.isEmpty()) {
                return;
            }
            int totalPosts = softChangeAuthorNamePostsBatch(postIds, newName);
        } catch (Exception e) {
            log.error("Error during posts cascade updating userName: {}",userId, e);
        }
    }

    private List<ObjectId> getIds(String collection, Criteria criteria) {
        Query query = new Query(criteria);
        List<ObjectId> ids = mongoTemplate.findDistinct(
                query,
                "_id",
                collection,
                ObjectId.class
        );

        log.info("Found {} posts to change name for user {}", ids.size(), criteria);
        return ids;
    }

    private int softChangeAuthorNamePostsBatch(List<ObjectId> postIds, String newName) {
        int totalChanged = 0;

        for (int i = 0; i < postIds.size(); i += BATCH_SIZE) {
            List<ObjectId> batch = postIds.subList(i, Math.min(i + BATCH_SIZE, postIds.size()));
            long changed = changeAuthorNameBatch("posts", Criteria.where("_id").in(batch), newName);
            totalChanged += changed;
            log.debug("change author name batch of {} posts", changed);
        }

        return totalChanged;
    }

    private long changeAuthorNameBatch(String collection, Criteria criteria, String newName) {
        Query query = new Query(criteria);
        Update update = new Update()
                .set("authorName", newName);

        return mongoTemplate.updateMulti(query, update, collection).getModifiedCount();
    }

    public String getUserNameFromObjectUserId(ObjectId objectUserId) {
        Long userId = objectIdToLong(objectUserId);
        return getUserName(userId);
    }

    public String getUserName(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            if(user.get().getLastName()==null)
                return user.get().getFirstName();
            return user.get().getFirstName()+" "+user.get().getLastName();
        }
        else  {
            return "Unknown user";
        }
    }

    public UserProfileResponseDTO getUserProfile(Long userId) {
        User user =  userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserProfileResponseDTO.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .numberOfFollowers(user.getNumberOfFollowers())
                .numberOfFollowing(user.getNumberOfFollowing())
                .createdAt(user.getCreatedAt())
                .aboutMe(user.getAbout())
                .build();
    }

    private Long objectIdToLong(ObjectId objectId) {
        String hex = objectId.toHexString();
        return new BigInteger(hex, 16).longValue();
    }

    private ObjectId longToObjectId(Long value) {
        return new ObjectId(String.format("%024x", value));
    }
}
