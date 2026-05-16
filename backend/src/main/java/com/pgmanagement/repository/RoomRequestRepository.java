package com.pgmanagement.repository;

import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.RoomRequest;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRequestRepository extends JpaRepository<RoomRequest, Long> {
    /** All requests submitted by a tenant, newest first. */
    List<RoomRequest> findByTenantOrderByCreatedAtDesc(User tenant);

    /** Pending requests across all PGs owned by the given owner. */
    List<RoomRequest> findByPg_OwnerAndStatusOrderByCreatedAtDesc(User owner, RoomRequest.Status status);

    /** All requests across all PGs owned by the given owner (any status). */
    List<RoomRequest> findByPg_OwnerOrderByCreatedAtDesc(User owner);

    /** Prevent duplicate pending requests by the same tenant for the same PG. */
    boolean existsByTenantAndPgAndStatus(User tenant, PG pg, RoomRequest.Status status);
}
