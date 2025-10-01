package com.example.blogs.controller;

import com.example.blogs.dto.PostCreateRequest;
import com.example.blogs.dto.PostResponse;
import com.example.blogs.dto.PostUpdateRequest;
import com.example.blogs.exception.UserIdResolver;
import com.example.blogs.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @RequestHeader("X-User-Id") String userHeader,
            @Valid @RequestBody PostCreateRequest req
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        return postService.create(userId, req);
    }

    @PutMapping("/{postId}")
    public PostResponse update(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId,
            @Valid @RequestBody PostUpdateRequest req
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        return postService.update(userId, postId, req);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        postService.softDelete(userId, postId);
    }

    @GetMapping("/{postId}")
    public PostResponse getOne(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        return postService.getOne(postId, userId);
    }

    // Feed: chỉ bài APPROVED
    @GetMapping("/feed")
    public Page<PostResponse> feed(Pageable pageable) {
        return postService.getFeed(pageable);
    }

    // Bài của chính mình (mọi trạng thái, trừ deleted)
    @GetMapping("/me")
    public Page<PostResponse> myPosts(
            @RequestHeader("X-User-Id") String userHeader,
            Pageable pageable
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        return postService.getMyPosts(userId, pageable);
    }

    // Like / Unlike
    @PostMapping("/{postId}/like")
    public long like(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        return postService.like(userId, postId);
    }

    @DeleteMapping("/{postId}/like")
    public long unlike(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        return postService.unlike(userId, postId);
    }
}