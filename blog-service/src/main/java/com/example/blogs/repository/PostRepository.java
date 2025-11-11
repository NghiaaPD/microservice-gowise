package com.example.blogs.repository;

import com.example.blogs.entity.Post;
import com.example.blogs.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query("select distinct p.category from Post p where p.deleted = false and p.category is not null order by p.category asc")
    List<String> findDistinctCategories();

    @Query("""
            select p from Post p
            where p.deleted = false
              and (
                    (:userId is null and p.status = com.example.blogs.entity.PostStatus.APPROVED)
                    or (:userId is not null and (p.status = com.example.blogs.entity.PostStatus.APPROVED or p.authorUserId = :userId))
                  )
              and (:category is null or lower(p.category) = :category)
              and (:search is null or lower(p.title) like :search)
            """)
    Page<Post> searchTimeline(
            @Param("userId") UUID userId,
            @Param("category") String category,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select coalesce(sum(p.viewCount), 0)
            from Post p
            where p.deleted = false and p.authorUserId = :authorId
            """)
    Long sumViewsByAuthor(@Param("authorId") UUID authorId);

    long countByDeletedFalseAndAuthorUserId(UUID authorUserId);
}
