package com.basketball.app.repository;

import com.basketball.app.model.Message;
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
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private MessageRepository repository;

    @Test
    void findsBySenderReceiverAndGroup() {
        Message message = new Message();
        message.setSenderId(1L);
        message.setReceiverId(2L);
        message.setGroupId(10L);
        message.setContent("hello");
        entityManager.persistAndFlush(message);

        assertEquals(1, repository.findBySenderId(1L).size());
        assertEquals(1, repository.findByReceiverId(2L).size());
        assertEquals(1, repository.findByGroupId(10L).size());
    }
}
