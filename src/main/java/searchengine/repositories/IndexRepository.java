package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    boolean existsByPage(PageEntity page);
    List<IndexEntity> findAllByLemma(LemmaEntity lemma);
    List<IndexEntity> findAllByLemmaAndPage(LemmaEntity lemma, PageEntity page);
}
