package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexPage;
import searchengine.dto.searching.QueryResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestBody IndexPage url) {
        return ResponseEntity.ok(indexingService.indexingPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<QueryResponse> search(@RequestParam String query,
                                                @RequestParam(required = false, defaultValue = "") String site,
                                                @RequestParam(required = false, defaultValue = "0") Integer offset,
                                                @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(searchingService.search(query, site, offset, limit));
    }
}