package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.QueryResponse;
import searchengine.services.SearchingService;
import searchengine.services.helpers.LemmaFinder;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {
    private LemmaFinder lemmaFinder;
    @Override
    public QueryResponse search(String query, String site, Integer offset, Integer limit) {
        try {
            lemmaFinder = new LemmaFinder();
        } catch (IOException ignored) {
        }
        Set<String> lemmas = lemmaFinder.getLemmaSet(query);

        return null;
    }
}
