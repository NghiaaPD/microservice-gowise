package com.example.user_service.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "friends")
@IdClass(Friend.FriendId.class)
public class Friend {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "friend_id", nullable = false)
    private UUID friendId;

    @Column(name = "status", nullable = false)
    private Boolean status = false;

    @Column(name = "is_sender", nullable = false)
    private Boolean isSender = false;

    // Constructors
    public Friend() {
    }

    public Friend(UUID userId, UUID friendId, Boolean status) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = status;
        this.isSender = false;
    }

    public Friend(UUID userId, UUID friendId, Boolean status, Boolean isSender) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = status;
        this.isSender = isSender;
    }

    // Getters and Setters

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getFriendId() {
        return friendId;
    }

    public void setFriendId(UUID friendId) {
        this.friendId = friendId;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getIsSender() {
        return isSender;
    }

    public void setIsSender(Boolean isSender) {
        this.isSender = isSender;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "userId=" + userId +
                ", friendId=" + friendId +
                ", status=" + status +
                ", isSender=" + isSender +
                '}';
    }

    // Composite Key Class
    public static class FriendId implements Serializable {
        private UUID userId;
        private UUID friendId;

        public FriendId() {
        }

        public FriendId(UUID userId, UUID friendId) {
            this.userId = userId;
            this.friendId = friendId;
        }

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public UUID getFriendId() {
            return friendId;
        }

        public void setFriendId(UUID friendId) {
            this.friendId = friendId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            FriendId friendId1 = (FriendId) o;
            return Objects.equals(userId, friendId1.userId) && Objects.equals(friendId, friendId1.friendId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, friendId);
        }
    }
}
