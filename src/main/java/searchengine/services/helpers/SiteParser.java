package searchengine.services.helpers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageData;
import searchengine.model.SiteData;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;

public class SiteParser {
    public static HashSet<String> parseSiteToLinks(String url, SiteData mainSite, PageRepository pageRepository, SiteRepository siteRepository) throws IOException, InterruptedException {
        if (!HelpingService.getDomain(url).equals(HelpingService.getDomain(mainSite.getUrl()))) return new HashSet<>();
        Connection.Response response;
        try {
            response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0")
                    .timeout(100000).ignoreHttpErrors(true).followRedirects(true)
                    .referrer("https://www.google.com/")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Document doc = response.parse();

        Elements links = doc.select("a[href]");
        HashSet<String> siteMap = new HashSet<>();

        for (Element link : links) {
            if (link.attr("abs:href").trim().contains(HelpingService.getNameDomain(mainSite.getUrl()))
                    && !link.attr("abs:href").trim().endsWith(HelpingService.getDomain(mainSite.getUrl()) + "/")
                    && !pageRepository.existsByPath(link.attr("abs:href").trim().endsWith("/") ?
                    link.attr("abs:href").trim() + "/" : link.attr("abs:href").trim())
                    && (!link.attr("abs:href").contains("rss")) && (!link.attr("href").contains("http"))
                    && link.attr("abs:href").contains("http")

            ) {
                if (!link.attr("abs:href").trim().contains("#")) {
                    System.out.println("Сохраняем " + link.attr("abs:href").trim());
                    PageData pageData = new PageData();
                    pageData.setSiteData(mainSite);
                    if (!link.attr("abs:href").trim().endsWith("/")) {
                        pageData.setPath(link.attr("abs:href").trim() + "/");
                    } else pageData.setPath(link.attr("abs:href").trim());
                    pageData.setCode(response.statusCode());
                    pageData.setContent(response.statusCode() == 200 ? doc.html() : "");
                    String addressForRepository = HelpingService.getAddressForRepository(pageData.getPath());
                    if (!pageRepository.existsByPath(addressForRepository)) {
                        String tempPath = pageData.getPath();
                        pageData.setPath(addressForRepository.substring(0, addressForRepository.length() - 1));
                        pageRepository.save(pageData);
                        mainSite.setStatusTime(new Date());
                        siteRepository.save(mainSite);
                        pageData.setPath(tempPath.trim());
                    }
                    siteMap.add(pageData.getPath());
                } else continue;
            }
            Thread.sleep(100);
        }

        return siteMap;
    }
}