package com.pgmanagement.repository;

import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PGRepository extends JpaRepository<PG, Long> {
    List<PG> findByOwner(User owner);
}
