package com.example.blogs.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "post")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_like_post_user", columnNames = {"post_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_post_likes_post", columnList = "post_id"),
                @Index(name = "idx_post_likes_user", columnList = "user_id")
        }
)
public class PostLike {

    @Id
    @GeneratedValue
    @UuidGenerator
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_like_post"))
    private Post post;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}