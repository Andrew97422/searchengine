package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "page", indexes = {@Index(columnList = "path", name = "path_idx")})
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "path", nullable = false, unique = true, length = 500)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT CHARSET utf8mb4", nullable = false)
    private String content;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteData siteData;
}
