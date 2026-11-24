package com.ozerler.marble.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.TotpBackupCode;

@Repository
public interface TotpBackupCodeRepository extends JpaRepository<TotpBackupCode, Long> {

    List<TotpBackupCode> findByUserIdAndUsedAtIsNull(Long userId);

    long countByUserIdAndUsedAtIsNull(Long userId);

    @Modifying
    @Query("DELETE FROM TotpBackupCode c WHERE c.userId = :userId")
    void deleteAllForUser(@Param("userId") Long userId);
}
