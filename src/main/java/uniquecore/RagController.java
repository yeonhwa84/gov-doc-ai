package uniquecore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class RagController {

    private static final String SYSTEM_PROMPT = """
            당신은 청소년복지 지원법 안내 도우미입니다.
            반드시 아래 [참고 조문]의 내용만 근거로 한국어로 간결하게 답하세요.
            답변에 근거 조문 번호를 함께 적으세요.
            참고 조문에 없는 내용은 '제공된 법령에서 확인할 수 없습니다'라고 답하세요.
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final double threshold;


    public RagController(ChatClient.Builder builder, VectorStore vectorStore,
                         @Value("${rag.similarity-threshold:0.5}") double threshold) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.threshold = threshold;
    }

    /** 화면에 돌려줄 근거 조문 */
    public record Source(String article, Double score, String text) {}

    /** 화면에 돌려줄 응답 (답변 + 근거 조문) */
    public record RagResponse(String answer, List<Source> sources) {}

    // 웹 화면(index.html)에서 호출하는 JSON API
    @PostMapping("/api/rag")
    public RagResponse ragApi(@RequestBody Map<String, String> body) {
        String q = body.getOrDefault("question", "").strip();
        if (q.isEmpty()) {
            return new RagResponse("질문을 입력해 주세요.", List.of());
        }

        List<Document> hits = search(q);
        String answer = generate(q, hits);

        List<Source> sources = hits.stream()
                .map(d -> new Source(
                        String.valueOf(d.getMetadata().get("article")),
                        d.getScore(),
                        d.getText()))
                .toList();

        return new RagResponse(answer, sources);
    }

    // 기존 주소창 테스트용 (그대로 유지)
    @GetMapping("/rag")
    public String rag(@RequestParam String q) {
        List<Document> hits = search(q);
        String answer = generate(q, hits);

        String sources = hits.stream()
                .map(d -> String.valueOf(d.getMetadata().get("article")))
                .collect(Collectors.joining(", "));

        return answer + "\n\n[검색된 조문] " + sources;
    }

    // 1) 질문과 가까운 조문 3개 검색 (기준값 미만은 제외)
    private List<Document> search(String q) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(q)
                        .topK(3)
                        .similarityThreshold(threshold)
                        .build());
    }


    // 2) 검색된 조문만 근거로 답변 생성
    private String generate(String q, List<Document> hits) {
        if (hits.isEmpty()) {
            return "제공된 법령에서 확인할 수 없습니다. (관련 조문 유사도가 기준값 "
                    + threshold + " 미만)";
        }

        String context = hits.stream()
                .map(d -> "[" + d.getMetadata().get("article") + "]\n" + d.getText())
                .collect(Collectors.joining("\n\n"));

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("[참고 조문]\n" + context + "\n\n[질문]\n" + q)
                .call()
                .content();
    }
}
