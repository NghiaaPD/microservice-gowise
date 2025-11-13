package com.example.user_service.service;

import com.example.user_service.entity.Friend;
import com.example.user_service.repository.FriendRepository;
import com.example.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FriendService {

    private static final Logger logger = LoggerFactory.getLogger(FriendService.class);

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Add friend - creates bidirectional friendship with status=false
     * 
     * @param userId   - ID of user sending friend request
     * @param friendId - ID of user receiving friend request
     * @return AddFriendResult with success status and message
     */
    public AddFriendResult addFriend(UUID userId, UUID friendId) {
        logger.info("Adding friend: userId={} wants to add friendId={}", userId, friendId);

        // Validate that both users exist
        if (!userRepository.existsById(userId)) {
            logger.warn("User with ID {} not found", userId);
            return new AddFriendResult(false, "User not found", "USER_NOT_FOUND");
        }

        if (!userRepository.existsById(friendId)) {
            logger.warn("Friend with ID {} not found", friendId);
            return new AddFriendResult(false, "Friend user not found", "FRIEND_NOT_FOUND");
        }

        // Check if users are trying to add themselves
        if (userId.equals(friendId)) {
            logger.warn("User {} trying to add themselves as friend", userId);
            return new AddFriendResult(false, "Cannot add yourself as friend", "SELF_ADD");
        }

        // Check if friendship already exists (either direction)
        Optional<Friend> existingFriendship1 = friendRepository.findByUserIdAndFriendId(userId, friendId);
        Optional<Friend> existingFriendship2 = friendRepository.findByUserIdAndFriendId(friendId, userId);

        if (existingFriendship1.isPresent() || existingFriendship2.isPresent()) {
            logger.warn("Friendship already exists between {} and {}", userId, friendId);
            Friend existing = existingFriendship1.orElse(existingFriendship2.get());
            if (existing.getStatus()) {
                return new AddFriendResult(false, "Already friends", "ALREADY_FRIENDS");
            } else {
                return new AddFriendResult(false, "Friend request already sent", "REQUEST_PENDING");
            }
        }

        try {
            // Create first record: user -> friend (status = false, is_sender = true)
            // This user is the one who SENT the friend request
            Friend friend1 = new Friend(userId, friendId, false, true);
            friendRepository.save(friend1);
            logger.info("Created friend record: userId={}, friendId={}, status=false, isSender=true", userId, friendId);

            // Create second record: friend -> user (status = false, is_sender = false)
            // This is the mirror record for the RECEIVER
            Friend friend2 = new Friend(friendId, userId, false, false);
            friendRepository.save(friend2);
            logger.info("Created friend record: userId={}, friendId={}, status=false, isSender=false", friendId,
                    userId);

            return new AddFriendResult(true, "Friend request sent successfully", null);
        } catch (Exception e) {
            logger.error("Error adding friend: {}", e.getMessage(), e);
            return new AddFriendResult(false, "Error adding friend: " + e.getMessage(), "DATABASE_ERROR");
        }
    }

    /**
     * Get all friends for a user
     */
    public List<Friend> getAllFriends(UUID userId) {
        logger.info("Getting all friends for userId={}", userId);
        return friendRepository.findAllByUserId(userId);
    }

    /**
     * Get pending friend requests for a user
     */
    public List<Friend> getPendingRequests(UUID userId) {
        logger.info("Getting pending friend requests for userId={}", userId);
        return friendRepository.findPendingRequestsByUserId(userId);
    }

    /**
     * Get accepted friends for a user
     */
    public List<Friend> getAcceptedFriends(UUID userId) {
        logger.info("Getting accepted friends for userId={}", userId);
        return friendRepository.findAcceptedFriendsByUserId(userId);
    }

    /**
     * Accept friend request - updates both records to status=true
     */
    public boolean acceptFriendRequest(UUID userId, UUID friendId) {
        logger.info("Accepting friend request: userId={} accepting friendId={}", userId, friendId);

        Optional<Friend> friendship1 = friendRepository.findByUserIdAndFriendId(userId, friendId);
        Optional<Friend> friendship2 = friendRepository.findByUserIdAndFriendId(friendId, userId);

        if (friendship1.isEmpty() || friendship2.isEmpty()) {
            logger.warn("Friendship not found between {} and {}", userId, friendId);
            return false;
        }

        try {
            // Update both records to status = true
            Friend friend1 = friendship1.get();
            friend1.setStatus(true);
            friendRepository.save(friend1);

            Friend friend2 = friendship2.get();
            friend2.setStatus(true);
            friendRepository.save(friend2);

            logger.info("Accepted friend request between {} and {}", userId, friendId);
            return true;
        } catch (Exception e) {
            logger.error("Error accepting friend request: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remove friend - deletes both records
     */
    public boolean removeFriend(UUID userId, UUID friendId) {
        logger.info("Removing friend: userId={} removing friendId={}", userId, friendId);

        Optional<Friend> friendship1 = friendRepository.findByUserIdAndFriendId(userId, friendId);
        Optional<Friend> friendship2 = friendRepository.findByUserIdAndFriendId(friendId, userId);

        if (friendship1.isEmpty() && friendship2.isEmpty()) {
            logger.warn("No friendship found between {} and {}", userId, friendId);
            return false;
        }

        try {
            friendship1.ifPresent(friendRepository::delete);
            friendship2.ifPresent(friendRepository::delete);
            logger.info("Removed friendship between {} and {}", userId, friendId);
            return true;
        } catch (Exception e) {
            logger.error("Error removing friend: {}", e.getMessage(), e);
            return false;
        }
    }

    // Response wrapper for addFriend operation
    public static class AddFriendResult {
        private boolean success;
        private String message;
        private String errorCode; // "ALREADY_EXISTS", "USER_NOT_FOUND", "SELF_ADD", etc.

        public AddFriendResult(boolean success, String message, String errorCode) {
            this.success = success;
            this.message = message;
            this.errorCode = errorCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
