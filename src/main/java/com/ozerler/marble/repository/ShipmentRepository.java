package com.ozerler.marble.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByWaybillNo(String waybillNo);

    boolean existsByWaybillNo(String waybillNo);

    Page<Shipment> findAllByOrderByDepartureTimeDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "project"})
    @Query("SELECT s FROM Shipment s ORDER BY s.departureTime DESC, s.id DESC")
    List<Shipment> findAllWithDetails();
}
