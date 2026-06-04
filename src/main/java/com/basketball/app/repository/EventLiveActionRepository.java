package com.basketball.app.repository;

import com.basketball.app.model.EventLiveAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventLiveActionRepository extends JpaRepository<EventLiveAction, Long> {
    List<EventLiveAction> findByEventIdOrderByCreatedAtAscIdAsc(Long eventId);
    List<EventLiveAction> findByEventIdAndOpponentJerseyNumberIn(Long eventId, List<Integer> opponentJerseyNumbers);
    Optional<EventLiveAction> findByIdAndEventId(Long id, Long eventId);
    void deleteByEventId(Long eventId);
}
