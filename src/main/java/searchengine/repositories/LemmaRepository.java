package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    boolean existsByLemma(String lemma);
    LemmaEntity findByLemma(String lemma);

    int countAllBySiteEntity(SiteEntity siteEntity);
}
