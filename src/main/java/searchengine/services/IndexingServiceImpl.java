package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteData;
import searchengine.model.StatusSite;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.helpers.HelpingService;
import searchengine.services.helpers.RecursiveLinkParser;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final List<Thread> threadList = new ArrayList<>();
    private final SitesList sites;
    ForkJoinPool pool = new ForkJoinPool();
    private volatile boolean isRunning = false;
    private volatile boolean isStopped = false;

    @Override
    public Map<String, Object> startIndexing() {
        if (isRunning && !isStopped) {
            Map<String, Object> response = new HashMap<>();
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return response;
        }
        isRunning = true;
        isStopped = false;
        pageRepository.deleteAll();
        siteRepository.deleteAll();
        System.out.println(Thread.currentThread().getName());

        for (Site site : sites.getSites()) {
            threadList.add(new Thread(() -> {
                SiteData siteData = SiteData.builder().url(site.getUrl()).status(StatusSite.INDEXING)
                        .name(site.getName()).statusTime(new Date()).lastError(null).build();
                System.out.println("Вошли в сайт " + site.getUrl());
                System.out.println("Домен сайта: " + HelpingService.getDomain(site.getUrl()));
                try {
                    Jsoup.connect(site.getUrl()).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0")
                            .timeout(100000).ignoreHttpErrors(true).followRedirects(true)
                            .execute();
                    siteRepository.save(siteData);
                } catch (IOException e) {
                    siteData.setStatus(StatusSite.FAILED);
                    siteData.setStatusTime(new Date());
                    siteData.setLastError(e.getLocalizedMessage());
                    siteRepository.save(siteData);
                }
                HashSet<String> hashSet = new HashSet<>();
                try {
                    hashSet = pool.invoke(new RecursiveLinkParser(siteData.getUrl(), siteData, pageRepository, siteRepository));
                    hashSet.forEach(System.out::println);
                    siteData.setStatus(StatusSite.INDEXED);
                } catch (RuntimeException e) {
                    siteData.setStatus(StatusSite.FAILED);
                    siteData.setStatusTime(new Date());
                    siteData.setLastError("Индексация остановлена пользователем");
                } catch (Exception e) {
                    siteData.setStatus(StatusSite.FAILED);
                    siteData.setStatusTime(new Date());
                    siteData.setLastError(e.getMessage());
                } finally {
                    hashSet.forEach(System.out::println);
                    System.out.println("Вышли из сайта " + siteData.getUrl());
                    siteRepository.save(siteData);
                }
            }));
        }
        for (Thread thread : threadList) {
            try {
                thread.start();
            } catch (IllegalThreadStateException ignored) {
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("result", true);
        return response;
    }

    @Override
    public Map<String, Object> stopIndexing() {
        if (!isRunning && isStopped) {
            Map<String, Object> response = new HashMap<>();
            response.put("result", false);
            response.put("error", "Индексация не запущена");
            isRunning = false;
            isStopped = true;
            return response;
        }
        try {
            pool.shutdownNow();
            isStopped = true;
            isRunning = false;
        } catch (RuntimeException ignored) {
        }
        Map<String, Object> response = new HashMap<>();
        response.put("result", true);
        return response;
    }
}
