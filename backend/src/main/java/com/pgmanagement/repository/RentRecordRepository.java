package com.pgmanagement.repository;

import com.pgmanagement.entity.RentRecord;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RentRecordRepository extends JpaRepository<RentRecord, Long> {
    List<RentRecord> findByTenant(User tenant);
    List<RentRecord> findByRoom_PgOwner(User owner);
    boolean existsByTenantAndMonth(User tenant, String month);
    List<RentRecord> findByTenantAndStatus(User tenant, RentRecord.RentStatus status);

    /** All pending rent records due on or before the given date, used by the reminder job. */
    List<RentRecord> findByStatusAndDueDateLessThanEqual(RentRecord.RentStatus status, LocalDate cutoff);

    /** Pending rent records for a single owner — used by manual reminder trigger. */
    List<RentRecord> findByRoom_PgOwnerAndStatus(User owner, RentRecord.RentStatus status);
}
