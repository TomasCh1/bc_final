package com.basketball.app.repository;

import com.basketball.app.model.EventNominatedPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventNominatedPlayerRepository extends JpaRepository<EventNominatedPlayer, Long> {
    List<EventNominatedPlayer> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
}
