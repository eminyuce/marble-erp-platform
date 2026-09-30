package com.ozerler.marble.repository;

import com.ozerler.marble.model.ApplicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ApplicationLogRepository extends JpaRepository<ApplicationLog, Long>, JpaSpecificationExecutor<ApplicationLog> {

    long countByLevelAndTimestampAfter(String level, LocalDateTime timestamp);

    long countByTimestampAfter(LocalDateTime timestamp);

    @Modifying
    @Query("DELETE FROM ApplicationLog a WHERE a.timestamp < :cutoff")
    int deleteLogsOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
