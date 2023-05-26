package searchengine.services.helpers;

import lombok.RequiredArgsConstructor;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
public class RecursiveLinkParser extends RecursiveTask<Set<String>> {
    private final String url;
    private final SiteEntity mainSite;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Override
    protected Set<String> compute() {
        HashSet<String> siteMap;
        List<RecursiveLinkParser> tasks = new ArrayList<>();
        try {
            siteMap = SiteParser.parseSiteToLinks(url, mainSite, pageRepository, siteRepository, lemmaRepository, indexRepository);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (String address : siteMap) {
            RecursiveLinkParser task = new RecursiveLinkParser(address, mainSite, pageRepository, siteRepository, lemmaRepository, indexRepository);
            task.fork();
            tasks.add(task);
        }

        for (RecursiveLinkParser task : tasks) {
            siteMap.addAll(task.join());
        }

        return siteMap;
    }
}
