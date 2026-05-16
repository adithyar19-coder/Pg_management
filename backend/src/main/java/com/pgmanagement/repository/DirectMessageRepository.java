package com.pgmanagement.repository;

import com.pgmanagement.entity.DirectMessage;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    /** Full chronological chat between two users (either direction). */
    @Query("SELECT m FROM DirectMessage m " +
           "WHERE (m.sender = :a AND m.recipient = :b) OR (m.sender = :b AND m.recipient = :a) " +
           "ORDER BY m.createdAt ASC")
    List<DirectMessage> findConversationBetween(@Param("a") User a, @Param("b") User b);

    /** Every message that involves a user (sender OR recipient), newest first. */
    @Query("SELECT m FROM DirectMessage m " +
           "WHERE m.sender = :u OR m.recipient = :u " +
           "ORDER BY m.createdAt DESC")
    List<DirectMessage> findAllInvolving(@Param("u") User u);

    long countByRecipientAndIsRead(User recipient, Boolean isRead);
}
