package com.basketball.app.repository;

import com.basketball.app.model.Export;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportRepository extends JpaRepository<Export, Long> {
    List<Export> findByCreatedBy(Long createdBy);
    List<Export> findByStatus(Export.ExportStatus status);
}

