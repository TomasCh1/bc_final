package com.basketball.app.repository;

import com.basketball.app.model.EventOpponentPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventOpponentPlayerRepository extends JpaRepository<EventOpponentPlayer, Long> {
    List<EventOpponentPlayer> findByEventIdOrderByJerseyNumberAsc(Long eventId);
    boolean existsByEventIdAndJerseyNumber(Long eventId, Integer jerseyNumber);
    void deleteByEventId(Long eventId);
}
