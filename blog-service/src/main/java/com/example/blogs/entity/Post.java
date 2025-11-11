package com.example.blogs.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "likes")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_status", columnList = "status"),
                @Index(name = "idx_posts_author", columnList = "author_user_id"),
                @Index(name = "idx_posts_deleted", columnList = "deleted"),
                @Index(name = "idx_posts_view_count", columnList = "view_count")
        }
)
public class Post {

    @Id
    @GeneratedValue
    @UuidGenerator
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.PENDING;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(length = 80)
    private String category = "Travel Stories";

    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    private Instant publishedAt;

    private UUID moderatedByUserId;
    @Column(length = 1000)
    private String moderationNote;
    private Instant moderatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostLike> likes = new HashSet<>();

    public void increaseViewCount() {
        this.viewCount++;
    }
}
