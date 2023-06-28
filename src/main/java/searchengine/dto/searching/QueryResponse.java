package searchengine.dto.searching;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QueryResponse {
    private boolean result;
    private int count;
    private List<DataDescription> data;
}
