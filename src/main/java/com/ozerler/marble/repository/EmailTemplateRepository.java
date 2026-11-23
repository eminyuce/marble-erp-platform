package com.ozerler.marble.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.web.GridColumnFilterRewriter;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByTemplateKey(String templateKey);
    boolean existsByTemplateKey(String templateKey);
    List<EmailTemplate> findAllByOrderByTemplateNameAsc();
    List<EmailTemplate> findByCategoryOrderByTemplateNameAsc(String category);
    long countByIsActiveTrue();

    @Query("SELECT DISTINCT t.category FROM EmailTemplate t")
    List<String> findDistinctCategories();

    @Query(value = "SELECT t FROM EmailTemplate t WHERE "
            + "(:category IS NULL OR t.category = :category) AND "
            + "(:active IS NULL OR t.isActive = :active) AND "
            + "(:search IS NULL OR LOWER(t.templateKey) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(t.templateName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(t.subject) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(COALESCE(t.category, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
            queryRewriter = GridColumnFilterRewriter.class)
    Page<EmailTemplate> searchTemplates(@Param("category") String category,
                                        @Param("active") Boolean active,
                                        @Param("search") String search,
                                        Pageable pageable);
}
