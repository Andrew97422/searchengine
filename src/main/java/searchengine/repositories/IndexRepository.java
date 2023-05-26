package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexData;

@Repository
public interface IndexRepository extends JpaRepository<IndexData, Integer> {
}
