package com.maplehub.analytics.controller;

import com.maplehub.analytics.model.AnalyticsEvent;
import com.maplehub.analytics.model.DashboardQuery;
import com.maplehub.analytics.model.WindowedAggregation;
import com.maplehub.analytics.repository.ClickHouseRepository;
import com.maplehub.analytics.service.EventIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final EventIngestionService ingestionService;
    private final ClickHouseRepository repository;

    public AnalyticsController(EventIngestionService ingestionService, ClickHouseRepository repository) {
        this.ingestionService = ingestionService;
        this.repository = repository;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> ingestEvent(@RequestBody AnalyticsEvent event) {
        ingestionService.ingest(event);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/events/batch")
    public ResponseEntity<Void> ingestBatch(@RequestBody List<AnalyticsEvent> events) {
        ingestionService.ingestBatch(events);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/dashboard")
    public List<WindowedAggregation> queryDashboard(DashboardQuery query) {
        return repository.query(query);
    }

    @GetMapping("/dashboard/top-events")
    public List<Map<String, Object>> topEvents(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "10") int limit) {
        return repository.topEvents(tenantId, limit);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
