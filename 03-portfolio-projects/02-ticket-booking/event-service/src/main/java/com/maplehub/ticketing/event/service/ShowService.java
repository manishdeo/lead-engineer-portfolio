package com.maplehub.ticketing.event.service;

import com.maplehub.ticketing.event.model.Show;
import com.maplehub.ticketing.event.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShowService {
    private final ShowRepository showRepository;

    public Page<Show> listActiveShows(Pageable pageable) {
        return showRepository.findByActiveTrue(pageable);
    }

    public Show getShow(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Show not found: " + id));
    }
}
