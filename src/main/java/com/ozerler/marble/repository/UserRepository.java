package com.ozerler.marble.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.User;
import com.ozerler.marble.web.GridColumnFilterRewriter;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    @Query(value = "SELECT u FROM User u WHERE u.deleted = false AND "
            + "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND "
            + "(:roleName IS NULL OR EXISTS (SELECT 1 FROM u.roles r WHERE r.name = :roleName)) AND "
            + "(:enabled IS NULL OR u.enabled = :enabled)",
            queryRewriter = GridColumnFilterRewriter.class)
    Page<User> searchActiveUsers(@Param("search") String search,
                                 @Param("roleName") String roleName,
                                 @Param("enabled") Boolean enabled,
                                 Pageable pageable);

    @EntityGraph(attributePaths = {"roles"})
    @Query(value = "SELECT u FROM User u WHERE u.deleted = false AND "
            + "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND "
            + "EXISTS (SELECT 1 FROM u.roles r WHERE r.name IN :roleNames) AND "
            + "(:enabled IS NULL OR u.enabled = :enabled)",
            queryRewriter = GridColumnFilterRewriter.class)
    Page<User> searchActiveUsersByAnyRole(@Param("search") String search,
                                          @Param("roleNames") Collection<String> roleNames,
                                          @Param("enabled") Boolean enabled,
                                          Pageable pageable);

    long countByDeletedFalse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :id")
    int incrementFailedLoginAttempts(@Param("id") Long id);

    @Query("SELECT u.failedLoginAttempts FROM User u WHERE u.id = :id")
    Integer findFailedLoginAttempts(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = :until WHERE u.id = :id")
    int lockAccount(@Param("id") Long id, @Param("until") LocalDateTime until);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = NULL WHERE u.id = :id")
    int resetLoginFailures(@Param("id") Long id);
}
