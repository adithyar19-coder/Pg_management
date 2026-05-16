package com.pgmanagement.repository;

import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PGRepository extends JpaRepository<PG, Long> {
    List<PG> findByOwner(User owner);

    /**
     * Tenant-facing search. Both filters are optional (null → ignored).
     * Case-insensitive contains-match on city and locality.
     */
    @Query("SELECT p FROM PG p " +
           "WHERE (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "  AND (:locality IS NULL OR LOWER(p.locality) LIKE LOWER(CONCAT('%', :locality, '%')))")
    List<PG> searchByLocation(@Param("city") String city, @Param("locality") String locality);
}
