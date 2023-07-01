package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexPage;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.searching.LemmaFinder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
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

        for (Site site : sites.getSites()) {
            threadList.add(new Thread(() -> {
                SiteEntity siteEntity = SiteEntity.builder().url(site.getUrl()).status(StatusSite.INDEXING)
                        .name(site.getName()).statusTime(new Date()).lastError(null).build();
                Connection.Response response;
                try {
                    response = Jsoup.connect(site.getUrl()).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0")
                            .timeout(100000).ignoreHttpErrors(true).followRedirects(true)
                            .execute();
                    siteRepository.save(siteEntity);
                    PageEntity pageEntity = PageEntity.builder().siteEntity(siteEntity).path("/")
                            .content(response.parse().html()).code(response.statusCode()).build();
                    pageRepository.save(pageEntity);
                } catch (IOException e) {
                    siteEntity.setStatus(StatusSite.FAILED);
                    siteEntity.setStatusTime(new Date());
                    siteEntity.setLastError(e.getLocalizedMessage());
                    siteRepository.save(siteEntity);
                }

                try {
                    pool.invoke(new RecursiveLinkParser(siteEntity.getUrl(), siteEntity, pageRepository, siteRepository, lemmaRepository, indexRepository));
                    siteEntity.setStatus(StatusSite.INDEXED);
                    Thread.sleep(100);
                } catch (RuntimeException | InterruptedException ignored) {
                } catch (Exception e) {
                    siteEntity.setStatus(StatusSite.FAILED);
                    siteEntity.setStatusTime(new Date());
                    siteEntity.setLastError(e.getLocalizedMessage());
                    e.printStackTrace();
                    pool.shutdownNow();
                    isStopped = true;
                    isRunning = false;
                }
                System.out.println("Вышли из сайта " + siteEntity.getUrl());
                siteRepository.save(siteEntity);
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
            Thread.sleep(100);
        } catch (RuntimeException | InterruptedException ignored) {
        }
        List<SiteEntity> siteEntities = siteRepository.findAll();
        for (SiteEntity siteEntity : siteEntities) {
            if (siteEntity.getStatus().equals(StatusSite.INDEXING)) {
                siteEntity.setLastError("Индексация остановлена пользователем");
                siteEntity.setStatus(StatusSite.FAILED);
                siteRepository.save(siteEntity);
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("result", true);
        return response;
    }

    @Override
    public Map<String, Object> indexingPage(IndexPage url) {
        String siteUrl = AddressChanging.getNameDomain(url.getUrl());

        for (String u : siteRepository.findAll().stream().map(SiteEntity::getUrl).collect(Collectors.toList())) {
            if (u.contains(siteUrl)) {
                SiteEntity mainSite = siteRepository.getByUrl(u);
                Connection.Response response;
                LemmaFinder lemmaFinder;
                try {
                    response = Jsoup.connect(url.getUrl()).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0")
                            .timeout(100000).ignoreHttpErrors(true).followRedirects(true)
                            .referrer("https://www.google.com/")
                            .execute();
                    lemmaFinder = new LemmaFinder();
                } catch (IOException e) {
                    Map<String, Object> response1 = new HashMap<>();
                    response1.put("result", false);
                    response1.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файл");
                    return response1;
                }
                PageEntity page;
                String newUrl;

                if (url.getUrl().endsWith("/")) {
                    newUrl = AddressChanging.getAddressForRepository(url.getUrl()).substring(0, url.getUrl().length() - 2);
                } else newUrl = AddressChanging.getAddressForRepository(url.getUrl())
                        .substring(0, AddressChanging.getAddressForRepository(url.getUrl()).length() - 1);

                if (!pageRepository.existsByPathAndSiteEntity(newUrl, siteRepository.getByUrl(u))) {
                    page = PageEntity.builder().code(response.statusCode()).content(response.body())
                            .path(newUrl).siteEntity(mainSite).build();
                    pageRepository.save(page);
                } else {
                    page = pageRepository.getByPathAndSiteEntity(newUrl, mainSite);
                }

                Map<String, Integer> lemmas = lemmaFinder.collectLemmas(LemmaFinder.cleanFromHtml(response.body()));
                for (String lemma : lemmas.keySet()) {
                    if (lemmaRepository.existsByLemma(lemma)) {
                        LemmaEntity lemmaEntity = lemmaRepository.findByLemma(lemma);
                        lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                        lemmaRepository.save(lemmaEntity);
                    } else {
                        LemmaEntity lemmaEntity = LemmaEntity.builder().lemma(lemma)
                                .siteEntity(siteRepository.getByUrl(u)).frequency(1).build();
                        lemmaRepository.save(lemmaEntity);
                    }
                    IndexEntity indexEntity = IndexEntity.builder().page(page)
                            .lemma(lemmaRepository.findByLemma(lemma)).rank(lemmas.get(lemma)).build();
                    try {
                        indexRepository.save(indexEntity);
                    } catch (Exception e) {
                        Map<String, Object> response1 = new HashMap<>();
                        response1.put("result", false);
                        response1.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файл");
                        return response1;
                    }
                }

                Map<String, Object> response1 = new HashMap<>();
                response1.put("result", true);
                return response1;
            }
        }
        Map<String, Object> response1 = new HashMap<>();
        response1.put("result", false);
        response1.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файл");
        return response1;
    }
}
