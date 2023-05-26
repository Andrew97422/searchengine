package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageData;

@Repository
public interface PageRepository extends JpaRepository<PageData, Integer> {
    boolean existsByPath(String path);
}
