package com.example.blogs.service.impl;

import com.example.blogs.dto.PostCreateRequest;
import com.example.blogs.dto.PostResponse;
import com.example.blogs.dto.PostUpdateRequest;
import com.example.blogs.dto.UserPostStatsResponse;
import com.example.blogs.entity.Post;
import com.example.blogs.entity.PostLike;
import com.example.blogs.entity.PostStatus;
import com.example.blogs.repository.PostLikeRepository;
import com.example.blogs.repository.PostRepository;
import com.example.blogs.service.PostService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepo;
    private final PostLikeRepository likeRepo;

    @Override
    public PostResponse create(UUID authorUserId, PostCreateRequest req) {
        Post p = new Post();
        p.setAuthorUserId(authorUserId);
        p.setTitle(req.getTitle());
        p.setContent(req.getContent());
        p.setStatus(PostStatus.PENDING);
        p.setCategory(normalizeCategory(req.getCategory()));
        p.setCoverImageUrl(normalizeCoverImage(req.getCoverImageUrl()));
        Post saved = postRepo.save(p);
        return toResponse(saved, 0);
    }

    @Override
    public PostResponse update(UUID userId, UUID postId, PostUpdateRequest req) {
        Post p = postRepo.findById(postId).orElseThrow(() -> notFound(postId));
        ensureOwner(userId, p);
        if (p.isDeleted()) throw new IllegalStateException("Post has been deleted");
        p.setTitle(req.getTitle());
        p.setContent(req.getContent());
        if (req.getCategory() != null) {
            p.setCategory(normalizeCategory(req.getCategory()));
        }
        if (req.getCoverImageUrl() != null) {
            p.setCoverImageUrl(normalizeCoverImage(req.getCoverImageUrl()));
        }
        // Policy: sửa bài -> quay về PENDING để duyệt lại
        p.setStatus(PostStatus.PENDING);
        p.setPublishedAt(null);
        Post saved = postRepo.save(p);
        long likes = likeRepo.countByPost(saved);
        return toResponse(saved, likes);
    }

    @Override
    public UserPostStatsResponse softDelete(UUID userId, UUID postId) {
        Post p = postRepo.findById(postId).orElseThrow(() -> notFound(postId));
        ensureOwner(userId, p);
        p.setDeleted(true);
        postRepo.save(p);
        return snapshotUserStats(userId);
    }

    @Override
    @Transactional
    public PostResponse getOne(UUID postId, @Nullable UUID requesterUserId) {
        Post p = postRepo.findById(postId).orElseThrow(() -> notFound(postId));
        // Xem bài:
        // - APPROVED: ai cũng xem
        // - Khác APPROVED: chỉ tác giả xem
        if (p.getStatus() != PostStatus.APPROVED) {
            if (requesterUserId == null || !p.getAuthorUserId().equals(requesterUserId)) {
                throw new SecurityException("You are not allowed to view this post");
            }
        }
        p.setViewCount(p.getViewCount() + 1);
        long likes = likeRepo.countByPost(p);
        return toResponse(p, likes);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(Pageable pageable) {
        return postRepo.findApproved(pageable)
                .map(p -> toResponse(p, likeRepo.countByPost(p)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getMyPosts(UUID userId, Pageable pageable) {
        return postRepo.findByDeletedFalseAndAuthorUserId(userId, pageable)
                .map(p -> toResponse(p, likeRepo.countByPost(p)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getTimeline(@Nullable UUID userId, String search, String category, Pageable pageable) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedCategory = normalizeCategoryFilter(category);
        String loweredCategory = toLowerCase(normalizedCategory);
        String likeSearch = normalizedSearch != null ? "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%" : null;
        return postRepo.searchTimeline(userId, loweredCategory, likeSearch, pageable)
                .map(p -> toResponse(p, likeRepo.countByPost(p)));
    }

    @Override
    public long like(UUID userId, UUID postId) {
        Post p = postRepo.findById(postId).orElseThrow(() -> notFound(postId));
        if (p.isDeleted()) throw new IllegalStateException("Post has been deleted");
        if (likeRepo.existsByPostIdAndUserId(postId, userId)) {
            return likeRepo.countByPost(p);
        }
        PostLike like = new PostLike();
        like.setPost(p);
        like.setUserId(userId);
        try {
            likeRepo.save(like);
        } catch (DataIntegrityViolationException e) {
            // unique (post,user) đã có — idempotent
        }
        return likeRepo.countByPost(p);
    }

    @Override
    public long unlike(UUID userId, UUID postId) {
        Post p = postRepo.findById(postId).orElseThrow(() -> notFound(postId));
        likeRepo.deleteByPostIdAndUserId(postId, userId);
        return likeRepo.countByPost(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveCategories() {
        List<String> categories = postRepo.findDistinctCategories();
        if (categories.isEmpty()) {
            return List.of("Travel Stories");
        }
        return categories;
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalViewsByAuthor(UUID userId) {
        Long totalViews = postRepo.sumViewsByAuthor(userId);
        return totalViews != null ? totalViews : 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalLikesByAuthor(UUID userId) {
        return likeRepo.countByAuthorUserId(userId);
    }

    // ===== helpers =====
    private RuntimeException notFound(UUID id) {
        return new EntityNotFoundException("Post not found: " + id);
    }

    private void ensureOwner(UUID userId, Post p) {
        if (!p.getAuthorUserId().equals(userId)) {
            throw new SecurityException("Not the author");
        }
    }

    private PostResponse toResponse(Post p, long likeCount) {
        return PostResponse.builder()
                .id(p.getId())
                .authorUserId(p.getAuthorUserId())
                .title(p.getTitle())
                .content(p.getContent())
                .status(p.getStatus())
                .likeCount(likeCount)
                .viewCount(p.getViewCount())
                .category(p.getCategory())
                .coverImageUrl(p.getCoverImageUrl())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .publishedAt(p.getPublishedAt())
                .build();
    }

    private UserPostStatsResponse snapshotUserStats(UUID userId) {
        long totalPosts = postRepo.countByDeletedFalseAndAuthorUserId(userId);
        long totalLikes = likeRepo.countByAuthorUserId(userId);
        long totalViews = countTotalViewsByAuthor(userId);
        return new UserPostStatsResponse(totalPosts, totalLikes, totalViews);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "Travel Stories";
        }
        return category.trim();
    }

    private String normalizeCategoryFilter(String category) {
        if (category == null) {
            return null;
        }
        String trimmed = category.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCoverImage(String coverImageUrl) {
        if (coverImageUrl == null || coverImageUrl.isBlank()) {
            return null;
        }
        return coverImageUrl.trim();
    }

    private String toLowerCase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
