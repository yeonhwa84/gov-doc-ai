package uniquecore;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final VectorStore vectorStore;

    public SearchController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // 예: /search?q=위기청소년의 정의는?&k=5
    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q,
                                            @RequestParam(defaultValue = "5") int k) {
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder().query(q).topK(k).build());

        return hits.stream()
                .map(d -> Map.<String, Object>of(
                        "article", d.getMetadata().get("article"),
                        "score", d.getScore(),
                        "preview", d.getText().substring(0, Math.min(80, d.getText().length()))))
                .toList();
    }
}