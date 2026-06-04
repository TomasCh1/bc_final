package com.basketball.app.repository;

import com.basketball.app.model.EventReferee;
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
class EventRefereeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private EventRefereeRepository repository;

    @Test
    void findByEventIdOrderBySortOrderAsc_ReturnsOrderedRows() {
        EventReferee r1 = new EventReferee();
        r1.setEventId(5L);
        r1.setSortOrder(1);
        r1.setName("Ref B");
        EventReferee r2 = new EventReferee();
        r2.setEventId(5L);
        r2.setSortOrder(0);
        r2.setName("Ref A");
        entityManager.persistAndFlush(r1);
        entityManager.persistAndFlush(r2);

        var result = repository.findByEventIdOrderBySortOrderAsc(5L);
        assertEquals("Ref A", result.get(0).getName());
        repository.deleteByEventId(5L);
        entityManager.flush();
        assertEquals(0, repository.findByEventIdOrderBySortOrderAsc(5L).size());
    }
}
