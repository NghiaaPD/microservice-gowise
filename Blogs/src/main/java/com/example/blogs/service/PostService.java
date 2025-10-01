package com.example.blogs.service;

import com.example.blogs.dto.PostCreateRequest;
import com.example.blogs.dto.PostUpdateRequest;
import com.example.blogs.dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PostService {

    PostResponse create(UUID authorUserId, PostCreateRequest req);

    PostResponse update(UUID userId, UUID postId, PostUpdateRequest req);

    void softDelete(UUID userId, UUID postId);

    PostResponse getOne(UUID postId, UUID requesterUserId);

    Page<PostResponse> getFeed(Pageable pageable); // APPROVED only

    Page<PostResponse> getMyPosts(UUID userId, Pageable pageable);

    long like(UUID userId, UUID postId);

    long unlike(UUID userId, UUID postId);
}