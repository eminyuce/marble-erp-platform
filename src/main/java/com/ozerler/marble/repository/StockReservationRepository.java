package com.ozerler.marble.repository;

import com.ozerler.marble.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByProjectId(Long projectId);

    List<StockReservation> findByStatus(String status);
}
