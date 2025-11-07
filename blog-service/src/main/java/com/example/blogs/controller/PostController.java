package com.example.blogs.controller;

import com.example.blogs.dto.*;
import com.example.blogs.exception.UserIdResolver;
import com.example.blogs.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

//    Create new blog
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> create(
            Authentication auth,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            @Valid @RequestBody PostCreateRequest req
    ) {
        UUID userId;
        if (StringUtils.hasText(userHeader)) {
            userId = UserIdResolver.requireUserId(userHeader);
            if (auth != null && auth.getPrincipal() instanceof UUID jwtUserId && !jwtUserId.equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "X-User-Id does not match JWT");
            }
        } else {
            if (auth == null || auth.getPrincipal() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication");
            }
            userId = (UUID) auth.getPrincipal();
        }

        PostResponse post = postService.create(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(post, Map.of("author", userId.toString())));
    }

//    Update blog
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> update(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId,
            @Valid @RequestBody PostUpdateRequest req
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        PostResponse body = postService.update(userId, postId, req);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(body));
    }

//    Soft Delete
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        postService.softDelete(userId, postId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(null, Map.of("message", "Deleted post!")));
    }

//    return a blog with user right
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getOne(
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            @PathVariable UUID postId
    ) {
        UUID userId = null;
        if (StringUtils.hasText(userHeader)) {
            userId = UserIdResolver.requireUserId(userHeader);
        }
        PostResponse body = postService.getOne(postId, userId);
        return  ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(body));
    }

    // Feed: chỉ bài APPROVED
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> feed(Pageable pageable) {
        Page<PostResponse> body = postService.getFeed(pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(PageResponse.from(body), Map.of("categories", postService.getActiveCategories())));
    }

    // Timeline: bài đã duyệt + bài của chính mình
    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> timeline(
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            Pageable pageable
    ) {
        UUID userId = null;
        if (StringUtils.hasText(userHeader)) {
            userId = UserIdResolver.requireUserId(userHeader);
        }
        Page<PostResponse> body = postService.getTimeline(userId, search, category, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(body), Map.of("categories", postService.getActiveCategories())));
    }

    // Bài của chính mình (mọi trạng thái, trừ deleted)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> myPosts(
            @RequestHeader("X-User-Id") String userHeader,
            Pageable pageable
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        Page<PostResponse> body = postService.getMyPosts(userId, pageable);
        long totalLikes = postService.countTotalLikesByAuthor(userId);
        long totalViews = postService.countTotalViewsByAuthor(userId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.ok(PageResponse.from(body), Map.of("totalLike", totalLikes, "totalViews", totalViews)));
    }

    // Like / Unlike
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Long>> like(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        long count = postService.like(userId, postId);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Long>> unlike(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID postId
    ) {
        UUID userId = UserIdResolver.requireUserId(userHeader);
        long count = postService.unlike(userId, postId);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }
}
