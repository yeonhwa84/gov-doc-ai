package uniquecore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvalController {

    private final VectorStore vectorStore;
    private final double threshold;

    public EvalController(VectorStore vectorStore,
                          @Value("${rag.similarity-threshold:0.5}") double threshold) {
        this.vectorStore = vectorStore;
        this.threshold = threshold;
    }

    /** 평가 질문 목록으로 검색 정확도를 측정한다. 예: /eval */
    @GetMapping("/eval")
    public Map<String, Object> eval() throws Exception {
        String text = new ClassPathResource("eval/eval-set.txt")
                .getContentAsString(StandardCharsets.UTF_8);

        List<Map<String, Object>> rows = new ArrayList<>();
        int total = 0, passed = 0;

        for (String line : text.split("\\R")) {
            if (line.isBlank() || !line.contains("|")) continue;
            String[] p = line.split("\\|");
            String question = p[0].strip();
            String expected = p[1].strip();

            List<Document> hits = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(3)
                            .similarityThreshold(threshold).build());

            List<String> articles = hits.stream()
                    .map(d -> String.valueOf(d.getMetadata().get("article"))).toList();
            Double topScore = hits.isEmpty() ? null : hits.get(0).getScore();

            boolean ok = "없음".equals(expected)
                    ? hits.isEmpty()                                            // 법에 없는 질문 → 아무것도 안 찾아야 정답
                    : !articles.isEmpty() && articles.get(0).equals(expected);  // 1등이 정답 조문이어야 정답

            total++;
            if (ok) passed++;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("result", ok ? "O" : "X");
            row.put("question", question);
            row.put("expected", expected);
            row.put("found", articles);
            row.put("topScore", topScore);
            rows.add(row);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("threshold", threshold);
        summary.put("score", passed + " / " + total);
        summary.put("accuracy", total == 0 ? "0%" : Math.round(passed * 100.0 / total) + "%");
        summary.put("details", rows);
        return summary;
    }
}
