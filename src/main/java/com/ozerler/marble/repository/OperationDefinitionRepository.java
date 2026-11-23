package com.ozerler.marble.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.OperationDefinition;

@Repository
public interface OperationDefinitionRepository extends JpaRepository<OperationDefinition, Long> {

    List<OperationDefinition> findByCategoryAndActiveTrueOrderByDisplayOrderAsc(String category);

    Optional<OperationDefinition> findByCode(String code);

    boolean existsByCode(String code);
}
