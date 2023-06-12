package searchengine.services.searching;

import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.util.ArrayList;
import java.util.List;

public class FindPages {
    public static List<PageEntity> getPagesList(List<String> lemmas,
                                                LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        return new ArrayList<>();
    }

    public static List<String> removeLemmasWithToMuchPages(List<String> lemmas,
                                                           LemmaRepository lemmaRepository, PageRepository pageRepository) {
        return new ArrayList<>();
    }
}
