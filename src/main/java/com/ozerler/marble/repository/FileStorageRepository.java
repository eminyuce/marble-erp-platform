package com.ozerler.marble.repository;

import com.ozerler.marble.model.FileStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage, Long> {

    List<FileStorage> findByEntityTypeAndEntityIdAndDeletedFalseOrderByCreatedDateAsc(String entityType, Long entityId);

    List<FileStorage> findByIdInAndDeletedFalse(Collection<Long> ids);

    Optional<FileStorage> findByIdAndDeletedFalse(Long id);

    Optional<FileStorage> findFirstByFileNameAndDeletedFalse(String fileName);

    Optional<FileStorage> findByObjectKeyAndDeletedFalse(String objectKey);

    List<FileStorage> findByDeletedFalse();

    List<FileStorage> findByEntityTypeAndEntityId(String entityType, Long entityId);

    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
}
