package com.maplehub.ticketing.event.controller;

import com.maplehub.ticketing.event.model.Show;
import com.maplehub.ticketing.event.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public ResponseEntity<Page<Show>> listShows(Pageable pageable) {
        return ResponseEntity.ok(showService.listActiveShows(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Show> getShow(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShow(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<?> getSeats(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShow(id).getSeatIds());
    }
}
