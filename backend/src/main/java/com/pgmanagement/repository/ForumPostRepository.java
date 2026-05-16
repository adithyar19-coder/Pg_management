package com.pgmanagement.repository;

import com.pgmanagement.entity.ForumPost;
import com.pgmanagement.entity.PG;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    /** Posts in a PG ordered with pinned first, then newest. */
    List<ForumPost> findByPgOrderByIsPinnedDescCreatedAtDesc(PG pg);
}
