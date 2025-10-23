package com.example.blogs.controller;

import com.example.blogs.dto.ApiResponse;
import com.example.blogs.dto.ModerateRequest;
import com.example.blogs.dto.PageResponse;
import com.example.blogs.dto.PostResponse;
import com.example.blogs.service.AdminPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminService;

    @GetMapping("/pending")
    public ApiResponse<PageResponse<PostResponse>> pending(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(adminService.listPending(pageable)));
    }

    @PostMapping("/{postId}/moderate")
    public ApiResponse<PostResponse> moderate(
            Authentication auth,
            @RequestHeader(value = "X-User-Id", required = false) String userHeader,
            @PathVariable UUID postId,
            @Valid @RequestBody ModerateRequest req
    ) {
        UUID adminUserId = (UUID) auth.getPrincipal();
        if (StringUtils.hasText(userHeader) && !adminUserId.equals(UUID.fromString(userHeader))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "X-User-Id does not match token");
        }

        String action = req.getAction().trim().toUpperCase();
        PostResponse result = switch (action) {
            case "APPROVE" -> adminService.approve(adminUserId, postId, req.getNote());
            case "REJECT"  -> adminService.reject(adminUserId, postId, req.getNote());
            default        -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "action must be APPROVE or REJECT");
        };

        return ApiResponse.ok(result);
    }
}
