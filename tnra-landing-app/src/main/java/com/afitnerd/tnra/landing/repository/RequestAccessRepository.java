package com.afitnerd.tnra.landing.repository;

import com.afitnerd.tnra.landing.model.RequestAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RequestAccessRepository extends JpaRepository<RequestAccess, Long> {

    @Query("SELECT COUNT(r) FROM RequestAccess r WHERE r.ipAddress = :ip AND r.submittedAt > :since")
    long countByIpAddressSince(@Param("ip") String ip, @Param("since") LocalDateTime since);
}
