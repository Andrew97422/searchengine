package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    boolean existsByPathAndSiteEntity(String path, SiteEntity siteEntity);
    PageEntity getByPathAndSiteEntity(String path, SiteEntity siteEntity);

    int countAllBySiteEntity(SiteEntity siteEntity);
    boolean existsBySiteEntity(SiteEntity siteEntity);
}
