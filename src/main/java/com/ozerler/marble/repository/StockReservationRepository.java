package com.ozerler.marble.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.StockReservation;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByProjectId(Long projectId);

    List<StockReservation> findByStatus(String status);
}
