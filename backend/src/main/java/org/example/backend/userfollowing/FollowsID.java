package org.example.backend.userfollowing;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class FollowsID implements Serializable {
    private Long followingUserId;
    private Long followedUserId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FollowsID followsID = (FollowsID) o;
        return Objects.equals(followingUserId, followsID.followingUserId) &&
                Objects.equals(followedUserId, followsID.followedUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followingUserId, followedUserId);
    }
}