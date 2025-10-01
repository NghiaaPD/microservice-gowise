package com.example.blogs.controller;

import com.example.blogs.dto.ModerateRequest;
import com.example.blogs.dto.PostResponse;
import com.example.blogs.exception.UserIdResolver;
import com.example.blogs.service.AdminPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminService;

    // Danh sách bài ở trạng thái PENDING
    @GetMapping("/pending")
    public Page<PostResponse> pending(Pageable pageable) {
        return adminService.listPending(pageable);
    }

    // Approve / Reject bằng một endpoint
    @PostMapping("/{postId}/moderate")
    public PostResponse moderate(
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            @PathVariable UUID postId,
            @Valid @RequestBody ModerateRequest req
    ) {
        UUID adminUserId = UserIdResolver.requireUserId(userHeader);
        String action = req.getAction().trim().toUpperCase();
        return switch (action) {
            case "APPROVE" -> adminService.approve(adminUserId, postId, req.getNote());
            case "REJECT" -> adminService.reject(adminUserId, postId, req.getNote());
            default -> throw new IllegalArgumentException("Action must be APPROVE or REJECT");
        };
    }
}