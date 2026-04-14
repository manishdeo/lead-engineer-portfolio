package com.maplehub.ticketing.event.repository;

import com.maplehub.ticketing.event.model.Show;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<Show, Long> {
    Page<Show> findByActiveTrue(Pageable pageable);
}
