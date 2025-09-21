package com.example.blogs.repository;

import com.example.blogs.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    //    Find by Slug (URL)
    Optional<Blog> findBySlug(String slug);

    //    Get list by authorId
    List<Blog> findByAuthorId(Long authorId);

    //    Get Blogs by status
    List<Blog> findByStatus(Blog.Status status);

    List<Blog> getByStatus(Blog.Status status);
}
