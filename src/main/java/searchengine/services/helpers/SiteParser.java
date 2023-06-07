package searchengine.services.helpers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

public class SiteParser {
    public static HashSet<String> parseSiteToLinks(String url, SiteEntity mainSite, PageRepository pageRepository, SiteRepository siteRepository,
                                                   LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException, InterruptedException {
        Connection.Response response;
        try {
            response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0")
                    .timeout(100000).ignoreHttpErrors(true).followRedirects(true)
                    .referrer("https://www.google.com/").ignoreContentType(true)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Document doc = response.parse();

        PageEntity page;
        String pathForPage = HelpingService.getAddressForRepository(url).endsWith("/") && !(HelpingService.getAddressForRepository(url).equals("/")) ?
                HelpingService.getAddressForRepository(url).substring(0, HelpingService.getAddressForRepository(url).length() - 1) :
                HelpingService.getAddressForRepository(url);

        synchronized (SiteParser.class) {
            if (pageRepository.existsByPathAndSiteEntity(pathForPage, mainSite)) {
                page = pageRepository.getByPathAndSiteEntity(pathForPage, mainSite);
            } else {
                page = PageEntity.builder().code(response.statusCode()).content(doc.html())
                        .path(pathForPage).siteEntity(mainSite).build();
                try {
                    pageRepository.save(page);
                } catch (Exception ignored) {
                }
            }
        }

        saveLemmas(response, doc, lemmaRepository, mainSite, page, indexRepository);

        Elements links = doc.select("a[href]");
        HashSet<String> siteMap = new HashSet<>();

        for (Element link : links) {
            if (
                    link.attr("abs:href").contains("http")
                    && (link.attr("href").startsWith("/") ||
                    (HelpingService.getAddressWithoutW(link.attr("abs:href")).trim().startsWith(HelpingService.getAddressWithoutW(mainSite.getUrl())))
                    ) && !(link.attr("href").matches("^#|/|$"))
            ) {
                PageEntity pageEntity = new PageEntity();
                pageEntity.setSiteEntity(mainSite);
                if (!link.attr("abs:href").trim().endsWith("/")) {
                    pageEntity.setPath(link.attr("abs:href").trim() + "/");
                } else pageEntity.setPath(link.attr("abs:href").trim());
                pageEntity.setCode(response.statusCode());
                pageEntity.setContent(response.statusCode() == 200 ? doc.html() : "");
                String addressForRepository = HelpingService.getAddressForRepository(pageEntity.getPath());
                synchronized (SiteParser.class) {
                    if (!pageRepository.existsByPathAndSiteEntity(addressForRepository.substring(0, addressForRepository.length() - 1), mainSite)) {
                        System.out.println("Сохраняем " + link.attr("abs:href") + ", отсутствует " + addressForRepository.substring(0, addressForRepository.length() - 1));
                        siteMap.add(link.attr("abs:href").trim());
                        pageEntity.setPath(addressForRepository.substring(0, addressForRepository.length() - 1));
                        try {
                            pageRepository.save(pageEntity);
                        } catch (Exception e) {
                            continue;
                        }
                        mainSite.setStatusTime(new Date());
                        siteRepository.save(mainSite);
                    }
                }
            }
            Thread.sleep(100);
        }

        return siteMap;
    }

    public static void saveLemmas(Connection.Response response, Document doc, LemmaRepository lemmaRepository,
                                  SiteEntity mainSite, PageEntity pageEntity, IndexRepository indexRepository) throws IOException {
        if (!(String.valueOf(response.statusCode()).startsWith("4") ||
                String.valueOf(response.statusCode()).startsWith("5"))) {
            Map<String, Integer> lemmas = new LemmaFinder().collectLemmas(LemmaFinder.getHtmlText(doc.html()));
            for (String lemma : lemmas.keySet()) {
                synchronized (SiteParser.class) {
                    if (lemmaRepository.existsByLemma(lemma)) {
                        try {
                            LemmaEntity lemmaEntity = lemmaRepository.findByLemma(lemma);
                            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                            lemmaRepository.save(lemmaEntity);
                        } catch (Exception e) {
                            continue;
                        }
                    } else {
                        LemmaEntity lemmaEntity = LemmaEntity.builder().lemma(lemma)
                                .siteEntity(mainSite).frequency(1).build();
                        lemmaRepository.save(lemmaEntity);
                    }
                    try {
                        IndexEntity indexEntity = IndexEntity.builder()
                                .page(pageEntity)
                                .lemma(lemmaRepository.findByLemma(lemma)).rank(lemmas.get(lemma)).build();
                        indexRepository.save(indexEntity);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}