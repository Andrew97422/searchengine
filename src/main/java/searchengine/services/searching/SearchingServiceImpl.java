package searchengine.services.searching;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.WrongCharaterException;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.DataDescription;
import searchengine.dto.searching.QueryResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {
    private LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    @Override
    public QueryResponse search(String query, String site, Integer offset, Integer limit) {
        String finalSite = site;
        if (site.endsWith("/")) {
            site = site.substring(0, site.length() - 1);
        }
        QueryResponse queryResponse = null;
        try {
            lemmaFinder = new LemmaFinder();
        } catch (IOException ignored) {
        }

        List<String> lemmasList = new ArrayList<>(lemmaFinder.getLemmaSet(query));
        //Отсеиваем неудачные леммы
        lemmasList.removeIf(lemma -> !lemmaRepository.existsByLemma(lemma));
        //Отсеиваем слишком часто встречающиеся леммы
        if (!finalSite.equals(""))    lemmasList.removeIf(lemma -> ((double) lemmaRepository.findByLemma(lemma).getFrequency() /
                pageRepository.countAllBySiteEntity(siteRepository.getByUrl(finalSite))) > 0.5);
        else {
            List<SiteEntity> siteEntities = siteRepository.findAll();
            for (SiteEntity siteEntity : siteEntities) {
                lemmasList.removeIf(lemma -> ((double) lemmaRepository.findByLemma(lemma).getFrequency() /
                        pageRepository.countAllBySiteEntity(siteEntity) > 0.2));
            }
        }

        if (lemmasList.size() == 0) {
            queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(false).build();
            return queryResponse;
        }

        if (lemmasList.size() != 1) {
            try {
                Comparator<String> comparator =
                        Comparator.comparing(i->lemmaRepository.findByLemma(i).getFrequency());
                lemmasList.sort(comparator);
            } catch (NullPointerException e) {
                queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(false).build();
                return queryResponse;
            }
        }

        List<PageEntity> listPages;
        try {
            listPages = findListPages(lemmasList.get(0));
        } catch (IndexOutOfBoundsException e) {
            queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(false).build();
            return queryResponse;
        }

        Set<PageEntity> finalSetPages = new HashSet<>();

        int count = 0;
        if (lemmasList.size() != 1) {
            for (int i = 1; i < lemmasList.size(); i++) {
                LemmaEntity lemmaEntity = lemmaRepository.findByLemma(lemmasList.get(i));
                for (int j = offset; j < listPages.size(); j++) {
                    if (indexRepository.existsByLemmaAndPage(lemmaEntity, listPages.get(j))) {
                        if (count < limit) {
                            finalSetPages.add(listPages.get(j));
                            count++;
                        }
                    }
                }
            }
        } else {
            LemmaEntity lemmaEntity = lemmaRepository.findByLemma(lemmasList.get(0));
            for (int j = offset; j < listPages.size(); j++) {
                if (indexRepository.existsByLemmaAndPage(lemmaEntity, listPages.get(j))) {
                    if (count < limit) {
                        finalSetPages.add(listPages.get(j));
                        count++;
                    }
                }
            }
        }

        if (finalSetPages.isEmpty()) {
            queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(true).build();
            return queryResponse;
        }

        List<PageEntity> finalListPages = new ArrayList<>(finalSetPages);

        // Calculating relative relevance
        List<Float> absRelevanceList = getAbsRelevanceList(finalListPages);
        List<Float> relRelevanceList = getRelRelevance(absRelevanceList);

        @Data
        @AllArgsConstructor
        class Results implements Comparable<Results> {
            PageEntity pageEntity;
            float absRelevance;
            float relRelevance;

            @Override
            public int compareTo(Results o) {
                return Float.compare(o.getRelRelevance(), this.getRelRelevance());
            }
        }

        List<Results> resultList = new ArrayList<>(finalListPages.size());
        for (int i = 0; i < finalListPages.size(); i++) {
            resultList.add(new Results(finalListPages.get(i), absRelevanceList.get(i), relRelevanceList.get(i)));
        }

        resultList.sort(Results::compareTo);

        List<PageEntity> pagesResult = resultList.stream().map(Results::getPageEntity).collect(Collectors.toList());
        pagesResult = new ArrayList<>(new HashSet<>(pagesResult));

        if (!siteRepository.existsByUrl(site)) {
            List<SiteEntity> sites = siteRepository.findAll();
            for (SiteEntity siteEntity : sites) {
                List<DataDescription> data = new ArrayList<>(pagesResult.size());
                int count1 = 0;  //count of the results
                int countEmpty = 0; //count empty snippets
                for (Results result : resultList) {
                    String snippet = getSnippet(result.getPageEntity(), lemmasList);
                    if (snippet.replaceAll(" ", "").equals("")) countEmpty++;
                    DataDescription description = DataDescription.builder().relevance(result.getRelRelevance())
                            .site(siteEntity.getUrl()).uri(result.getPageEntity().getPath())
                            .siteName(siteEntity.getName()).title(LemmaFinder.getTitle(result.getPageEntity().getContent()))
                            .snippet(snippet).build();
                    data.add(description);
                    count1++;
                    if (count1 >= limit) break;
                }
                if (countEmpty != 0) {
                    data.removeIf(record -> record.getSnippet().equals(""));
                }
                queryResponse = QueryResponse.builder().result(true).count(data.size()).data(data).build();
            }
        } else if (siteRepository.existsByUrl(site)){
            try {
                List<DataDescription> data = new ArrayList<>(pagesResult.size());
                int count1 = 0;  //count of the results
                int countEmpty = 0; //count empty snippets
                for (Results result : resultList) {
                    String snippet = getSnippet(result.getPageEntity(), lemmasList);
                    if (snippet.replaceAll(" ", "").equals("")) countEmpty++;
                    DataDescription description = DataDescription.builder().relevance(result.getRelRelevance())
                            .site(site).uri(result.getPageEntity().getPath())
                            .siteName(result.getPageEntity().getSiteEntity().getName())
                            .title(LemmaFinder.getTitle(result.getPageEntity().getContent()))
                            .snippet(snippet).build();
                    data.add(description);
                    count1++;
                    if (count1 >= limit) break;
                }
                if (countEmpty != 0) {
                    data.removeIf(record -> record.getSnippet().equals(""));
                }
                queryResponse = QueryResponse.builder().result(true).count(data.size()).data(data).build();
            } catch (Exception ignored){}
        }
        return queryResponse;
    }

    private List<PageEntity> findListPages(String lemma) throws IndexOutOfBoundsException {
        LemmaEntity lemmaEntity = lemmaRepository.findByLemma(lemma);
        return indexRepository.findAllByLemma(lemmaEntity)
                .stream().map(IndexEntity::getPage).collect(Collectors.toList());
    }

    private List<Float> getAbsRelevanceList(List<PageEntity> pages) {
        List<Float> resultList = new ArrayList<>(pages.size());
        for (PageEntity page : pages) {
            float result = 0;
            Set<String> lemmas = lemmaFinder.getLemmaSet(LemmaFinder.cleanFromHtml(page.getContent()));
            for (String lemma : lemmas) {
                LemmaEntity lemmaEntity = lemmaRepository.findByLemma(lemma);
                try {
                    result += indexRepository.findByLemmaAndPage(lemmaEntity, page).getRank();
                } catch (Exception ignored) {
                }
            }
            resultList.add(result);
        }
        return resultList;
    }

    private List<Float> getRelRelevance(List<Float> absRelevance) {
        if (absRelevance.size() == 1) {
            return absRelevance.stream().map(i->i/absRelevance.get(0)).collect(Collectors.toList());
        }
        float maxElement = Collections.max(absRelevance);
        return absRelevance.stream().map(i->i/maxElement).collect(Collectors.toList());
    }

    private String getSnippet(PageEntity page, List<String> lemmas) {
        String content = page.getContent();
        String contentWithNoHtml = LemmaFinder.cleanFromHtml(content);
        String[] sentences = contentWithNoHtml.split("\\p{Punct}");
        //String[] newSentences = new String[sentences.length];
        List<String> newSentences = new ArrayList<>();

        for (int i = 0; i < sentences.length; i++) {
            if (sentences[i].trim().equals(""))    continue;
            if (Character.isUpperCase(sentences[i].charAt(0))) {
                newSentences.add(sentences[i]);
            } else {
                try {
                    newSentences.set(i, newSentences.get(i).concat(sentences[i]).concat(" "));
                } catch (IndexOutOfBoundsException e) {
                    newSentences.add(sentences[i] + " ");
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        System.out.println(newSentences.size());
        List<String> sentencesWeNeed = new ArrayList<>();

        for (String newSentence : newSentences) {
            for (String lemma : lemmas) {
                if (lemmaFinder.getLemmaSet(newSentence).contains(lemma)) {
                    sentencesWeNeed.add(newSentence);
                }
            }
        }

        for (String sentence : sentencesWeNeed) {
            String[] words = sentence.split("\\s");
            for (String word : words) {
                String wordSmall = word.toLowerCase();
                try {
                    if (lemmas.contains(lemmaFinder.getNormalForm(wordSmall))) {
                        builder.append("<b>").append(word).append("</b> ");
                    } else {
                        builder.append(word).append(" ");
                    }
                } catch (IndexOutOfBoundsException | WrongCharaterException ignored) {}
            }
        }

        StringBuilder result = new StringBuilder();
        String[] splitString = builder.toString().split(" ");
        int length = 0;
        int count = 0;
        for (String word : splitString) {
            if (count >= 3)  break;
            if (length <= 75) {
                result.append(word).append(" ");
                length += word.length();
                length += 1;
            } else {
                result.append("\n").append(word).append(" ");
                length = 0;
                length += word.length();
                length += 1;
                count++;
            }
        }

        return result.toString();
    }
}
