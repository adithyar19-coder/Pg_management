package com.pgmanagement.repository;

import com.pgmanagement.entity.Complaint;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByTenantOrderByCreatedAtDesc(User tenant);
    @Query("SELECT c FROM Complaint c WHERE c.pg.owner = :owner ORDER BY c.createdAt DESC")
    List<Complaint> findByPgOwnerOrderByCreatedAtDesc(User owner);
    long countByPgOwnerAndStatus(User owner, Complaint.ComplaintStatus status);
}
