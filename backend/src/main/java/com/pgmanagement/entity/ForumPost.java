package com.pgmanagement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A community post on a PG's forum. Any tenant of that PG (or its owner) can create.
 * Owner can pin (sticky to top) and delete (moderation).
 */
@Entity
@Table(name = "forum_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pg_id", nullable = false)
    private PG pg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Builder.Default
    private Boolean isPinned = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Reply count — computed from ForumReplyRepository, not persisted. */
    @Transient
    private Integer replyCount;
}
