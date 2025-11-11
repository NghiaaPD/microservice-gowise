package com.example.blogs.repository;

import com.example.blogs.entity.Post;
import com.example.blogs.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    long countByPost(Post post);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    long deleteByPostIdAndUserId(UUID postId, UUID userId);

    @Query("""
            select count(pl) from PostLike pl
            where pl.post.deleted = false
              and pl.post.authorUserId = :authorId
            """)
    long countByAuthorUserId(@Param("authorId") UUID authorId);
}
