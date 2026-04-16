package com.pgmanagement.repository;

import com.pgmanagement.entity.Notification;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndIsRead(User user, Boolean isRead);
}
