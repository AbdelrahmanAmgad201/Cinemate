package org.example.backend.userfollowing;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.user.User;
import org.example.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserRepository userRepository;
    private final FollowsRepository followsRepository;

    @Transactional
    public void follow(Long followingUserId, Long followedUserId){
        if(followingUserId.equals(followedUserId)) return;
        FollowsID followsId = getFollowsID(followingUserId, followedUserId);
        Optional<Follows> optionalFollow =  followsRepository.findById(followsId);
        if(optionalFollow.isPresent()){
            refollow(optionalFollow.get());
            return;
        }
        newFollow(followingUserId, followedUserId, followsId);
    }

    @Transactional
    public void unfollow(Long followingUserId, Long followedUserId){
        FollowsID followsId = getFollowsID(followingUserId, followedUserId);
        Optional<Follows> optionalFollow =  followsRepository.findById(followsId);
        if(optionalFollow.isPresent()){
            Follows follows = optionalFollow.get();

            if(follows.getIsDeleted())    return;
            User followingUser = userRepository.findById(followingUserId)
                    .orElseThrow(()->new RuntimeException("User not found"));
            User followedUser = userRepository.findById(followedUserId)
                    .orElseThrow(()->new RuntimeException("User not found"));
            followingUser.setNumberOfFollowing(followingUser.getNumberOfFollowing()-1);
            followedUser.setNumberOfFollowers(followedUser.getNumberOfFollowers()-1);
            follows.setIsDeleted(true);
            userRepository.save(followingUser);
            userRepository.save(followedUser);
            followsRepository.save(follows);
        }
    }

    @Transactional
    public Boolean isFollowed(Long followingUserId, Long followedUserId){
        FollowsID followsId = getFollowsID(followingUserId, followedUserId);
        Optional<Follows> optionalFollow =  followsRepository.findById(followsId);
        if(optionalFollow.isPresent()){
            Follows follows = optionalFollow.get();
            return !follows.getIsDeleted();
        }
        return false;
    }

    @Transactional
    public Page<FollowerView> getUserFollowers(Long followedUserId, Pageable pageable){
        return followsRepository.findAllByFollowedUser_IdAndIsDeletedFalse(followedUserId,pageable);
    }

    @Transactional
    public Page<FollowingView> getUserFollowings(Long followingUserId, Pageable pageable){
        return followsRepository.findAllByFollowingUser_IdAndIsDeletedFalse(followingUserId,pageable);
    }

    private void newFollow(Long followingUserId, Long followedUserId,FollowsID followsId){
        User followingUser = userRepository.findById(followingUserId)
                .orElseThrow(()->new RuntimeException("User not found"));

        User followedUser = userRepository.findById(followedUserId)
                .orElseThrow(()->new RuntimeException("User not found"));

        followingUser.setNumberOfFollowing(followingUser.getNumberOfFollowing()+1);
        followedUser.setNumberOfFollowers(followedUser.getNumberOfFollowers()+1);

        Follows follows = Follows.builder()
                .followsID(followsId)
                .followingUser(followingUser)
                .followedUser(followedUser)
                .build();

        userRepository.save(followingUser);
        userRepository.save(followedUser);
        followsRepository.save(follows);
    }

    private void refollow(Follows follow){
        if(follow.getIsDeleted()){
            follow.setIsDeleted(false);
            follow.setFollowedAt(LocalDateTime.now());
            followsRepository.save(follow);
        }
    }

    private FollowsID getFollowsID(Long followingUserId, Long followedUserId){
        return FollowsID.builder()
                .followedUserId(followedUserId)
                .followingUserId(followingUserId)
                .build();
    }
}
