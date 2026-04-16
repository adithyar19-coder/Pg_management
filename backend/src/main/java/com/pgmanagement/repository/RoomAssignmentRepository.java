package com.pgmanagement.repository;

import com.pgmanagement.entity.Room;
import com.pgmanagement.entity.RoomAssignment;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, Long> {
    Optional<RoomAssignment> findByTenantAndIsActive(User tenant, Boolean isActive);
    List<RoomAssignment> findByRoomAndIsActive(Room room, Boolean isActive);
    List<RoomAssignment> findByRoom_PgOwnerAndIsActive(User owner, Boolean isActive);
}
