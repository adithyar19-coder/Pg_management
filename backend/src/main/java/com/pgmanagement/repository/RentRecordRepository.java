package com.pgmanagement.repository;

import com.pgmanagement.entity.RentRecord;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RentRecordRepository extends JpaRepository<RentRecord, Long> {
    List<RentRecord> findByTenant(User tenant);
    List<RentRecord> findByRoom_PgOwner(User owner);
    boolean existsByTenantAndMonth(User tenant, String month);
    List<RentRecord> findByTenantAndStatus(User tenant, RentRecord.RentStatus status);
}
