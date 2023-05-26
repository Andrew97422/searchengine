package searchengine.services.helpers;

import lombok.RequiredArgsConstructor;
import searchengine.model.PageData;
import searchengine.model.SiteData;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
public class RecursiveLinkParser extends RecursiveTask<HashSet<String>> {
    private final String url;
    private final SiteData mainSite;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    @Override
    protected HashSet<String> compute() {
        HashSet<String> siteMap;
        List<RecursiveLinkParser> tasks = new ArrayList<>();
        try {
            siteMap = SiteParser.parseSiteToLinks(url, mainSite, pageRepository, siteRepository);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (String address : siteMap) {
            RecursiveLinkParser task = new RecursiveLinkParser(address, mainSite, pageRepository, siteRepository);
            task.fork();
            tasks.add(task);
        }

        for (RecursiveLinkParser task : tasks) {
            siteMap.addAll(task.join());
        }

        return siteMap;
    }
}
