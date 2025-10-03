package com.example.blogs.dto;

import com.example.blogs.entity.PostStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class PostResponse {
    UUID id;
    UUID authorUserId;
    String title;
    String content;
    PostStatus status;
    long likeCount;
}

