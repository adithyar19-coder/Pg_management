package com.pgmanagement.repository;

import com.pgmanagement.entity.ForumPost;
import com.pgmanagement.entity.ForumReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    List<ForumReply> findByPostOrderByCreatedAtAsc(ForumPost post);
    long countByPost(ForumPost post);
    List<ForumReply> findByPostInOrderByCreatedAtAsc(List<ForumPost> posts);
}
