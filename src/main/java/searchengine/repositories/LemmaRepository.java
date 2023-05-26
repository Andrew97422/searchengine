package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaData;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaData, Integer> {
}
