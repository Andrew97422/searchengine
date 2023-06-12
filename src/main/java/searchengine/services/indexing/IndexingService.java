package searchengine.services.indexing;

import searchengine.dto.indexing.IndexPage;

import java.util.Map;

public interface IndexingService {
    Map<String, Object> startIndexing();

    Map<String, Object> stopIndexing();

    Map<String, Object> indexingPage(IndexPage url);
}
