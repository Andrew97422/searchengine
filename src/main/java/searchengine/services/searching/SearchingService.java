package searchengine.services.searching;

import searchengine.dto.searching.QueryResponse;

public interface SearchingService {
    QueryResponse search(String query, String site, Integer offset, Integer limit);
}
