package com.example.blogs.service;

import com.example.blogs.dto.PostCreateRequest;
import com.example.blogs.dto.PostResponse;
import com.example.blogs.dto.PostUpdateRequest;
import com.example.blogs.dto.UserPostStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import java.util.UUID;

public interface PostService {

    PostResponse create(UUID authorUserId, PostCreateRequest req);

    PostResponse update(UUID userId, UUID postId, PostUpdateRequest req);

    UserPostStatsResponse softDelete(UUID userId, UUID postId);

    PostResponse getOne(UUID postId, @Nullable UUID requesterUserId);

    Page<PostResponse> getFeed(Pageable pageable); // APPROVED only

    Page<PostResponse> getMyPosts(UUID userId, Pageable pageable);

    Page<PostResponse> getTimeline(@Nullable UUID userId, String search, String category, Pageable pageable);

    long like(UUID userId, UUID postId);

    long unlike(UUID userId, UUID postId);

    java.util.List<String> getActiveCategories();

    long countTotalViewsByAuthor(UUID userId);

    long countTotalLikesByAuthor(UUID userId);
}
