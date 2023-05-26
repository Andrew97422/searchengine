package searchengine.services;

import java.util.Map;

public interface IndexingService {
    Map<String, Object> startIndexing();

    Map<String, Object> stopIndexing();
}
