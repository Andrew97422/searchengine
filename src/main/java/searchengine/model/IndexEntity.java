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
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @OneToOne
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity page;

    @OneToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaEntity lemma;

    @Column(name = "ranking", nullable = false, columnDefinition = "FLOAT")
    private float rank;
}
