package com.example.blogs.service;

import com.example.blogs.entity.Blog;
import com.example.blogs.repository.BlogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BlogService {
    private final BlogRepository repo;

    public BlogService(BlogRepository repo) {
        this.repo = repo;
    }

    public List<Blog> getAllBlogs() {
        return repo.getByStatus(Blog.Status.APPROVED);
    }

    public Optional<Blog> getBlogById(Long id) {
        return repo.findById(id);
    }

    public Optional<Blog> getBlogBySlug(String slug) {
        return repo.findBySlug(slug);
    }

    public List<Blog> getBlogByAuthor(Long authorId) {
        return repo.findByAuthorId(authorId);
    }

    public Blog createBlog(Blog blog) {
        return repo.save(blog);
    }

    public Blog updateBlog(Long id, Blog updateBlog) {
        return repo.findById(id).map(blog -> {
            blog.setTitle(updateBlog.getTitle());
            blog.setContent(updateBlog.getContent());
            blog.setSlug(updateBlog.getSlug());
            blog.setStatus(updateBlog.getStatus());
            return repo.save(blog);
        }).orElseThrow(() -> new RuntimeException("Blog not found!"));

    }

    public void deleteBlog(Long id) {
        repo.deleteById(id);
    }

    //    Admin
    public List<Blog> getPendingBlogs() {
        return repo.getByStatus(Blog.Status.PENDING);
    }

    public Blog approveBlog(Long id) {
        return repo.findById(id).map(blog -> {
            blog.setStatus(Blog.Status.APPROVED);
            return repo.save(blog);
        }).orElseThrow(() -> new RuntimeException("Blog not found!"));

    }

    public Blog rejectBlog(Long id) {
        return repo.findById(id).map(blog -> {
            blog.setStatus(Blog.Status.REJECTED);
            return repo.save(blog);
        }).orElseThrow(() -> new RuntimeException("Blog not found!"));
    }
}
