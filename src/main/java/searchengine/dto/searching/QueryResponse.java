package searchengine.dto.searching;

import lombok.Data;

import java.util.List;

@Data
public class QueryResponse {
    private boolean result;
    private int count;
    private List<DataDescription> data;
}
