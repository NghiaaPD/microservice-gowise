package com.example.blogs.service;

import com.example.blogs.dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminPostService {

    Page<PostResponse> listPending(Pageable pageable);

    PostResponse approve(UUID adminUserId, UUID postId, String note);

    PostResponse reject(UUID adminUserId, UUID postId, String note);
}