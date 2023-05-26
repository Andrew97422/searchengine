package searchengine;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.services.helpers.LemmaFinder;

import java.io.IOException;

public class TestMain {
    public static void main(String[] args) throws IOException {
        //LemmaFinder finder = new LemmaFinder(new RussianLuceneMorphology());
        //System.out.println(finder.collectLemmas("Повторное появление леопарда Leo в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа."));
        //System.out.println(finder.getLemmaSet("Повторное появление леопарда Leo в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа."));
        String url = "https://www.google.com/search?q=%D0%BF%D0%B5%D1%80%D0%B5%D0%B2%D0%BE%D0%B4%D1%87%D0%B8%D0%BA&oq=%D0%BF%D0%B5&aqs=chrome.0.69i59j69i57j35i39i650j0i433i512l2j69i61l3.577j0j7&sourceid=chrome&ie=UTF-8";
        Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0")
                .timeout(100000).ignoreHttpErrors(true).followRedirects(true)
                .referrer("https://www.google.com/")
                .get();

    }
}
