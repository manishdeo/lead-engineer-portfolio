package com.maplehub.ticketing.booking.repository;

import com.maplehub.ticketing.booking.model.EventStoreEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventStoreRepository extends JpaRepository<EventStoreEntry, Long> {
    List<EventStoreEntry> findByAggregateIdOrderByVersionAsc(String aggregateId);
    int countByAggregateId(String aggregateId);
}
