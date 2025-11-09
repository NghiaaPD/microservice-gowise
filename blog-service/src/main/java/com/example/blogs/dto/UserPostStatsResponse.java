package com.example.blogs.dto;

public record UserPostStatsResponse(
        long totalPosts,
        long totalLikes,
        long totalViews
) {
}
