package com.example.user_service.repository;

import com.example.user_service.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Friend.FriendId> {

    /**
     * Check if friendship already exists between two users
     */
    @Query("SELECT f FROM Friend f WHERE f.userId = :userId AND f.friendId = :friendId")
    Optional<Friend> findByUserIdAndFriendId(@Param("userId") UUID userId, @Param("friendId") UUID friendId);

    /**
     * Get all friends for a user
     */
    @Query("SELECT f FROM Friend f WHERE f.userId = :userId")
    List<Friend> findAllByUserId(@Param("userId") UUID userId);

    /**
     * Get all pending friend requests for a user (received requests only)
     * Returns requests where the user is the RECEIVER (is_sender = false)
     */
    @Query("SELECT f FROM Friend f WHERE f.userId = :userId AND f.status = false AND f.isSender = false")
    List<Friend> findPendingRequestsByUserId(@Param("userId") UUID userId);

    /**
     * Get all accepted friends for a user
     */
    @Query("SELECT f FROM Friend f WHERE f.userId = :userId AND f.status = true")
    List<Friend> findAcceptedFriendsByUserId(@Param("userId") UUID userId);
}
