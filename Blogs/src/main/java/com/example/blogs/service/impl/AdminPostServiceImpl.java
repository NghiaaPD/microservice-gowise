package com.example.blogs.service.impl;

import com.example.blogs.dto.PostResponse;
import com.example.blogs.entity.Post;
import com.example.blogs.entity.PostStatus;
import com.example.blogs.repository.PostLikeRepository;
import com.example.blogs.repository.PostRepository;
import com.example.blogs.service.AdminPostService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminPostServiceImpl implements AdminPostService {

    private final PostRepository postRepo;
    private final PostLikeRepository likeRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> listPending(Pageable pageable) {
        return postRepo.findByDeletedFalseAndStatusIn(
                Arrays.asList(PostStatus.PENDING),
                pageable
        ).map(p -> toResponse(p, likeRepo.countByPost(p)));
    }

    @Override
    public PostResponse approve(UUID adminUserId, UUID postId, String note) {
        Post p = postRepo.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post not found"));
        p.setStatus(PostStatus.APPROVED);
        p.setPublishedAt(Instant.now());
        p.setModeratedByUserId(adminUserId);
        p.setModerationNote(note);
        p.setModeratedAt(Instant.now());
        Post saved = postRepo.save(p);
        return toResponse(saved, likeRepo.countByPost(saved));
    }

    @Override
    public PostResponse reject(UUID adminUserId, UUID postId, String note) {
        Post p = postRepo.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post not found"));
        p.setStatus(PostStatus.REJECTED);
        p.setPublishedAt(null);
        p.setModeratedByUserId(adminUserId);
        p.setModerationNote(note);
        p.setModeratedAt(Instant.now());
        Post saved = postRepo.save(p);
        return toResponse(saved, likeRepo.countByPost(saved));
    }

    private PostResponse toResponse(Post p, long likeCount) {
        return PostResponse.builder()
                .id(p.getId())
                .authorUserId(p.getAuthorUserId())
                .title(p.getTitle())
                .content(p.getContent())
                .status(p.getStatus())
                .deleted(p.isDeleted())
                .publishedAt(p.getPublishedAt())
                .likeCount(likeCount)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}