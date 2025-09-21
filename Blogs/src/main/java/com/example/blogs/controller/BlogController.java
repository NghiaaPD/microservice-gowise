package com.example.blogs.controller;

import com.example.blogs.dto.ApiResponse;
import com.example.blogs.entity.Blog;
import com.example.blogs.service.BlogService;
import com.example.blogs.utils.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/blogs")
public class BlogController {
    private final BlogService service;
    private final JWTUtils jWTUtils;

    public BlogController(BlogService service, JWTUtils jWTUtils) {
        this.service = service;
        this.jWTUtils = jWTUtils;
    }

    // ========== USER ==========
    @GetMapping
    public ResponseEntity<ApiResponse<List<Blog>>> getAllBlogs() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllBlogs(), "Fetch all blogs!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Blog>> getBlogById(@PathVariable Long id) {
        Blog blog = service.getBlogById(id)
                .orElseThrow(() -> new RuntimeException("Blog not found!"));
        return ResponseEntity.ok(ApiResponse.ok(blog, "Fetched blog successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Blog>> createBlog(@RequestBody Blog blog, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        Long id = jWTUtils.extractUserId(token);
        blog.setAuthorId(id);
        return ResponseEntity.ok(ApiResponse.ok(service.createBlog(blog), "Blog created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Blog>> updateBlog(@PathVariable Long id, @RequestBody Blog blog) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateBlog(id, blog), "Blog updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable Long id) {
        service.deleteBlog(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Blog deleted successfully"));
    }

    // =========== ADMIN ==========
    @GetMapping("/admin/pending")
    public ResponseEntity<ApiResponse<List<Blog>>> getPendingBlogs() {
        return ResponseEntity.ok(ApiResponse.ok(service.getPendingBlogs(), "Fetched pending blogs"));
    }

    @PutMapping("/admin/{id}/approve")
    public ResponseEntity<ApiResponse<Blog>> approveBlog(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.approveBlog(id), "Blog approved successfully"));
    }

    @PutMapping("/admin/{id}/reject")
    public ResponseEntity<ApiResponse<Blog>> rejectBlog(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.rejectBlog(id), "Blog rejected successfully"));
    }
}
