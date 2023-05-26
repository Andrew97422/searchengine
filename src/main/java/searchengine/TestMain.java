package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestMain {
    public static void main(String[] args) throws IOException {
        /*LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        List<String> wordBaseForms = luceneMorph.getNormalForms("котяра");
        wordBaseForms.forEach(System.out::println);*/
        String text = "The Playboy is a graphic novel by Canadian cartoonist Chester Brown (pictured), serialized in 1990 in his comic book Yummy Fur and collected in different revised book editions in 1992 and 2013. It deals with Brown's guilt and anxiety over his obsessive masturbation to Playboy Playmate models; out of fear of being caught, he repeatedly rids himself of copies of the magazine, only to retrieve them later. His conflicting emotions follow him into adulthood until he purges them by revealing himself through his comics. The free, organic arrangement of odd-shaped panels of simple, expressive artwork contrasts with his more detailed grid-like pages in his 1980s work, such as Ed the Happy Clown. The Playboy forms part of Brown's autobiographical period during the early 1990s, and was the first book-length work he planned as a complete story. It has attracted praise for its revealing honesty, and criticism from those who saw it as glorifying pornography. ";
        System.out.println(fff(text));
    }

    public Map<String, Integer> lemmas(String text) {
        Map<String, Integer> result = new HashMap<>();
        //String result = "";
        String[] words = text.split("[-\\s+]");
        int count = words.length;
        for (int i = 0; i < count; i++) {
            String tmp = words[i].replaceAll("[.,;0-9]", "");
            if (tmp != "") {
                /*result += tmp;
                if (i < count - 1) {
                    result += System.lineSeparator();
                }*/
            }
        }
        return result;
    }

    public static String fff(String text) {
        return text;
    }
}
