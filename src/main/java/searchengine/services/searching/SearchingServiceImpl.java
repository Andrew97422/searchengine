package searchengine.services.searching;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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
        QueryResponse queryResponse = null;
        try {
            lemmaFinder = new LemmaFinder();
        } catch (IOException ignored) {
        }

        List<String> lemmasList = new ArrayList<>(lemmaFinder.getLemmaSet(query));
        try {
            Comparator<String> comparator =
                    Comparator.comparing(i->lemmaRepository.findByLemma(i).getFrequency());
            lemmasList = lemmasList.stream().sorted(comparator).collect(Collectors.toList());
        } catch (NullPointerException e) {
            queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(false).build();
            return queryResponse;
        }

        List<PageEntity> listPages;
        try {
             listPages = findListPages(lemmasList.get(0));
        } catch (IndexOutOfBoundsException e) {
            queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(false).build();
            return queryResponse;
        }
        List<PageEntity> finalListPages = new ArrayList<>();

        for (int i = 1; i < lemmasList.size(); i++) {
            LemmaEntity lemmaEntity = lemmaRepository.findByLemma(lemmasList.get(i));
            for (int j = offset; j < listPages.size(); j++) {
                if (indexRepository.existsByLemmaAndPage(lemmaEntity, listPages.get(j))) {
                    if (!finalListPages.contains(listPages.get(j))) finalListPages.add(listPages.get(j));
                }
            }
        }

        if (finalListPages.isEmpty()) {
            queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(false).build();
            return queryResponse;
        }

        // Calculating relative relevance
        List<Float> absRelevanceList = getAbsRelevanceList(finalListPages);
        List<Float> relRelevanceList = getRelRelevance(absRelevanceList);

        finalListPages.stream().map(PageEntity::getPath).forEach(i->System.out.print(i + "\t"));
        System.out.println();
        absRelevanceList.forEach(i-> System.out.print(i + "\t"));
        System.out.println();
        relRelevanceList.forEach(i-> System.out.print(i + "\t"));

        @Data
        @AllArgsConstructor
        class Results implements Comparable<Results> {
            PageEntity pageEntity;
            float absRelevance;
            float relRelevance;

            @Override
            public int compareTo(Results o) {
                return Float.compare (o.getRelRelevance(), this.getRelRelevance());
            }
        }

        List<Results> resultList = new ArrayList<>(finalListPages.size());
        for (int i = 0; i < finalListPages.size(); i++) {
            resultList.add(new Results(finalListPages.get(i), absRelevanceList.get(i), relRelevanceList.get(i)));
        }

        resultList = resultList.stream().sorted(Results::compareTo).collect(Collectors.toList());

        List<PageEntity> pagesResult = resultList.stream().map(Results::getPageEntity).collect(Collectors.toList());
        HashSet<PageEntity> setPages = new HashSet<>(pagesResult);
        pagesResult = new ArrayList<>(setPages);

        if (!siteRepository.existsByUrl(site)) {
            List<SiteEntity> sites = siteRepository.findAll();
            for (SiteEntity siteEntity : sites) {
                List<DataDescription> data = new ArrayList<>(pagesResult.size());
                int count = 0;
                while (count <= limit) {
                    for (Results result : resultList) {
                        String snippet = getSnippet(result.getPageEntity(), lemmasList);
                        if (snippet.equals("")) continue;
                        DataDescription description = DataDescription.builder().relevance(result.getRelRelevance())
                                .site(siteEntity.getUrl()).uri(result.getPageEntity().getPath())
                                .siteName(siteEntity.getName()).title(LemmaFinder.getTitle(result.getPageEntity().getContent()))
                                .snippet(snippet).build();
                        data.add(description);
                        count++;
                    }
                }
                if (data.isEmpty()) {
                    queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(false).build();
                    return queryResponse;
                }
                queryResponse = QueryResponse.builder().result(true).count(data.size()).data(data).build();
            }
        } else if (siteRepository.existsByUrl(site)){
            try {
                List<DataDescription> data = new ArrayList<>(pagesResult.size());
                int count = 0;
                while (count < limit) {
                    for (Results result : resultList) {
                        String snippet = getSnippet(result.getPageEntity(), lemmasList);
                        if (snippet.equals("")) continue;
                        DataDescription description = DataDescription.builder().relevance(result.getRelRelevance())
                                .site(site).uri(result.getPageEntity().getPath())
                                .siteName(result.getPageEntity().getSiteEntity().getName())
                                .title(LemmaFinder.getTitle(result.getPageEntity().getContent()))
                                .snippet(snippet).build();
                        data.add(description);
                        count++;
                    }
                }
                if (data.isEmpty()) {
                    queryResponse = QueryResponse.builder().data(new ArrayList<>()).count(0).result(false).build();
                    return queryResponse;
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
        //System.out.println(LemmaFinder.cleanFromHtml(page.getContent()));
        //return "";

        String content = page.getContent();
        String contentWithNoHtml = LemmaFinder.cleanFromHtml(content);
        String[] sentences = contentWithNoHtml.split(" ");
        String[] newSentences = new String[sentences.length];
        Arrays.fill(newSentences, "");
        int j = 0;  //Counter for newSentences
        for (String sentence : sentences) {
            if (!Objects.equals(sentence, "") && Character.isUpperCase(sentence.charAt(0))) {
                newSentences[j] = sentence;
                j++;
            } else {
                newSentences[j] = newSentences[j].concat(sentence);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (String sentence : newSentences) {
            Set<String> lemmasSet = lemmaFinder.getLemmaSet(sentence);
            /*if (lemmasSet.containsAll(lemmas)) {
                String[] words = sentence.split("\\p{Punct}");
                for (String word : words) {
                    for (String lemma : lemmas) {
                        if (lemmaFinder.getNormalForm(word.toLowerCase()).equals(lemma)) {
                            builder.append("<b>").append(word).append("</b>").append(" ");
                        } else builder.append(word).append(" ");
                        System.out.println(word);
                    }
                }
            }*/
            for (String lemma : lemmas) {
                if (lemmasSet.contains(lemma)) {
                    String[] words = sentence.split("\\p{Punct}");
                    for (String word : words) {
                        if (lemmaFinder.getNormalForm(word.toLowerCase()).equals(lemma)) {
                            builder.append("<b>").append(word).append("</b>");
                        } else builder.append(word);
                        System.out.println(word);
                    }
                }
            }
        }
        return builder.toString();
    }
}
