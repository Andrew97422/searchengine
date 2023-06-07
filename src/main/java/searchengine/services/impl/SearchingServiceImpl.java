package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.QueryResponse;
import searchengine.services.SearchingService;

@Service
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {
    @Override
    public QueryResponse search(String query, String site, Integer offset, Integer limit) {

        return null;
    }
}
