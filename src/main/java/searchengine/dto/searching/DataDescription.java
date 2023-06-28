package searchengine.dto.searching;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataDescription {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;
}
