package com.ozerler.marble.repository;

import com.ozerler.marble.model.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByWaybillNo(String waybillNo);

    boolean existsByWaybillNo(String waybillNo);

    Page<Shipment> findAllByOrderByDepartureTimeDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer", "project"})
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Shipment s ORDER BY s.departureTime DESC, s.id DESC")
    List<Shipment> findAllWithDetails();
}
