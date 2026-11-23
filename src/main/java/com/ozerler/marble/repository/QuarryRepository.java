package com.ozerler.marble.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.web.GridColumnFilterRewriter;

@Repository
public interface QuarryRepository extends JpaRepository<Quarry, Long> {
    Optional<Quarry> findByCode(String code);

    @Query("SELECT q FROM Quarry q WHERE LOWER(q.code) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(q.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Quarry> searchByCodeOrName(@Param("query") String query, Pageable pageable);

    List<Quarry> findAllByOrderByCodeAsc();

    @Query(value = "SELECT q FROM Quarry q WHERE (:search IS NULL "
            + "OR LOWER(q.code) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(q.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(COALESCE(q.location, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
            queryRewriter = GridColumnFilterRewriter.class)
    Page<Quarry> search(@Param("search") String search, Pageable pageable);
}
