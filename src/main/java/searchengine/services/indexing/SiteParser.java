package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.searching.LemmaFinder;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SiteParser {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public HashSet<String> parseSiteToLinks(String url, SiteEntity mainSite) throws IOException, InterruptedException {
        Connection.Response response;
        try {
            response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0")
                    .timeout(100000).ignoreHttpErrors(true).followRedirects(true)
                    .referrer("https://www.google.com/").ignoreContentType(true)
                    .execute();
        } catch (IOException e) {
            PageEntity pageEntity = PageEntity.builder().siteEntity(mainSite).code(404)
                    .content("").path(AddressChanging.getAddressForRepository(url)).build();
            pageRepository.save(pageEntity);
            return new HashSet<>();
        }
        Document doc = response.parse();

        PageEntity page;
        String pathForPage = AddressChanging.getAddressForRepository(url).endsWith("/")
                && !(AddressChanging.getAddressForRepository(url).equals("/")) ?
                AddressChanging.getAddressForRepository(url).substring(0, AddressChanging.getAddressForRepository(url).length() - 1) :
                AddressChanging.getAddressForRepository(url);

        page = pageRepository.getByPathAndSiteEntity(pathForPage, mainSite);

        if (response.statusCode() % 100 != 4 && response.statusCode() % 100 != 5) {
            saveLemmas(doc.html(), lemmaRepository, page, indexRepository);
        }

        Elements links = doc.select("a[href]");
        HashSet<String> siteMap = new HashSet<>();

        for (Element link : links) {
            if (
                    link.attr("abs:href").contains("http")
                            && (link.attr("href").startsWith("/") ||
                            (AddressChanging.getAddressWithoutW(link.attr("abs:href")).trim().
                                    startsWith(AddressChanging.getAddressWithoutW(mainSite.getUrl())))
                    ) && !(link.attr("href").matches("^#|/|$"))
            ) {
                PageEntity pageEntity = new PageEntity();
                pageEntity.setSiteEntity(mainSite);
                if (!link.attr("abs:href").trim().endsWith("/")) {
                    pageEntity.setPath(link.attr("abs:href").trim() + "/");
                } else pageEntity.setPath(link.attr("abs:href").trim());
                if (Objects.equals(pageEntity.getPath().trim().replaceAll(" ", ""), ""))  return new HashSet<>();
                pageEntity.setCode(response.statusCode());
                pageEntity.setContent(response.statusCode() == 200 ? doc.html() : "");
                String addressForRepository = AddressChanging.getAddressForRepository(pageEntity.getPath());

                synchronized (PageRepository.class) {
                    if (!pageRepository.existsByPathAndSiteEntity(addressForRepository.substring(0, addressForRepository.length() - 1),
                            mainSite)) {
                        //System.out.println(Thread.currentThread() + " сохраняет " + link.attr("abs:href"));
                        siteMap.add(link.attr("abs:href").trim());
                        pageEntity.setPath(addressForRepository.substring(0, addressForRepository.length() - 1));
                        try {
                            pageRepository.save(pageEntity);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(1);
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
    public void saveLemmas(String html, LemmaRepository lemmaRepository,
                           PageEntity pageEntity, IndexRepository indexRepository) throws IOException {
        Map<String, Integer> lemmas = new LemmaFinder().collectLemmas(LemmaFinder.cleanFromHtml(html));

        for (String lemma : lemmas.keySet()) {
            synchronized (LemmaRepository.class) {
                try {
                    LemmaEntity lemmaEntity = lemmaRepository.findByLemma(lemma);
                    lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                    lemmaRepository.save(lemmaEntity);
                } catch (Exception e) {
                    LemmaEntity lemmaEntity = LemmaEntity.builder().lemma(lemma)
                            .siteEntity(pageEntity.getSiteEntity()).frequency(1).build();
                    lemmaRepository.save(lemmaEntity);
                }
            }

            try {
                IndexEntity indexEntity = IndexEntity.builder()
                        .page(pageEntity).lemma(lemmaRepository.findByLemma(lemma))
                        .rank((float) lemmas.get(lemma) / lemmas.size() * 100).build();
                indexRepository.save(indexEntity);
            } catch (Exception ignored) {
            }
        }
    }
}