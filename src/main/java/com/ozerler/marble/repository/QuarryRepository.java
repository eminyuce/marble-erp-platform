package com.ozerler.marble.repository;

import com.ozerler.marble.model.Quarry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuarryRepository extends JpaRepository<Quarry, Long> {
    Optional<Quarry> findByCode(String code);
}
