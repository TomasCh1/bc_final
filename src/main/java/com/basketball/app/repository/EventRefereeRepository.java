package com.basketball.app.repository;

import com.basketball.app.model.EventReferee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRefereeRepository extends JpaRepository<EventReferee, Long> {
    List<EventReferee> findByEventIdOrderBySortOrderAsc(Long eventId);

    void deleteByEventId(Long eventId);
}
