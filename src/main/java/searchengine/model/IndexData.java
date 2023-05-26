package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "indexing")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexData {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @JoinColumn(name = "page_id", nullable = false)
    private int pageId;

    @JoinColumn(name = "lemma_id", nullable = false)
    private int lemmaId;

    @Column(name = "ranking", nullable = false, columnDefinition = "FLOAT")
    private float rank;
}
