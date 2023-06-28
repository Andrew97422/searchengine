package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sites;
    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();

        for (int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = 0;
            int lemmas = 0;
            try {
                pages = pageRepository.countAllBySiteEntity(siteRepository.getByUrl(sitesList.get(i).getUrl()));
                lemmas = lemmaRepository.countAllBySiteEntity(siteRepository.getByUrl(sitesList.get(i).getUrl()));
                item.setStatus(siteRepository.getByUrl(sitesList.get(i).getUrl()).getStatus().toString());
                item.setError(siteRepository.getByUrl(sitesList.get(i).getUrl()).getLastError());
                item.setStatusTime(siteRepository.getByUrl(sitesList.get(i).getUrl()).getStatusTime().getTime());
            } catch (NullPointerException e) {
                item.setStatus("");
                item.setError("");
                item.setStatus(new Date().toString());
            }
            item.setPages(pages);
            item.setLemmas(lemmas);

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
