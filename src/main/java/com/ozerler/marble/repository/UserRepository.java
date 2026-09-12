package com.ozerler.marble.repository;

import com.ozerler.marble.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u WHERE u.deleted = false AND " +
           "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:roleName IS NULL OR EXISTS (SELECT 1 FROM u.roles r WHERE r.name = :roleName)) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> searchActiveUsers(@Param("search") String search,
                                 @Param("roleName") String roleName,
                                 @Param("enabled") Boolean enabled,
                                 Pageable pageable);

    long countByDeletedFalse();
}
