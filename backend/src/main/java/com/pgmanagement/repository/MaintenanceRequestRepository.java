package com.pgmanagement.repository;

import com.pgmanagement.entity.MaintenanceRequest;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByTenantOrderByCreatedAtDesc(User tenant);
    @Query("SELECT m FROM MaintenanceRequest m WHERE m.room.pg.owner = :owner ORDER BY m.createdAt DESC")
    List<MaintenanceRequest> findByRoomPgOwnerOrderByCreatedAtDesc(User owner);
}
