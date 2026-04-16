package com.pgmanagement.repository;

import com.pgmanagement.entity.Announcement;
import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByOwnerOrderByCreatedAtDesc(User owner);
    List<Announcement> findByPgInOrderByCreatedAtDesc(List<PG> pgs);
}
