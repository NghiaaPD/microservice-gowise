package com.example.blogs.repository;

import com.example.blogs.entity.Post;
import com.example.blogs.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findByDeletedFalseAndStatus(PostStatus status, Pageable pageable);

    Page<Post> findByDeletedFalseAndAuthorUserId(UUID authorUserId, Pageable pageable);

    // admin
    Page<Post> findByDeletedFalseAndStatusIn(Iterable<PostStatus> statuses, Pageable pageable);

    // bài đã duyệt
    default Page<Post> findApproved(Pageable pageable) {
        return findByDeletedFalseAndStatus(PostStatus.APPROVED, pageable);
    }

    // viewCount
    Optional<Post> findByIdAndDeletedFalse(UUID id);
}