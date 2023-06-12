package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.QueryResponse;
import searchengine.model.LemmaEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {
    private LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    @Override
    public QueryResponse search(String query, String site, Integer offset, Integer limit) {
        try {
            lemmaFinder = new LemmaFinder();
        } catch (IOException ignored) {
        }

        List<String> lemmasList = new ArrayList<>(lemmaFinder.getLemmaSet(query));
        Comparator<String> comparator =
                Comparator.comparing(i->lemmaRepository.findByLemma(i).getFrequency());
        lemmasList = lemmasList.stream().sorted(comparator).collect(Collectors.toList());

        /**TODO
         * Реализовать способ получения процента отсеивания лемм
         * */


        System.out.println(lemmasList);
        return null;
    }
}
