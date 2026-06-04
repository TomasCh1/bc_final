package com.basketball.app.repository;

import com.basketball.app.model.Export;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Tag("integration")
class ExportRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private ExportRepository repository;

    @Test
    void findsByCreatorAndStatus() {
        Export export = new Export();
        export.setType("attendance");
        export.setFormat(Export.ExportFormat.EXCEL);
        export.setStatus(Export.ExportStatus.COMPLETED);
        export.setCreatedBy(5L);
        entityManager.persistAndFlush(export);

        assertEquals(1, repository.findByCreatedBy(5L).size());
        assertEquals(1, repository.findByStatus(Export.ExportStatus.COMPLETED).size());
    }
}
