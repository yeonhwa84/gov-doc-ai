package uniquecore;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/rag")
    public String rag(@RequestParam String q) {
        // 1) 질문과 가까운 조문 3개 검색
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder().query(q).topK(3).build());

        String context = hits.stream()
                .map(d -> "[" + d.getMetadata().get("article") + "]\n" + d.getText())
                .collect(Collectors.joining("\n\n"));

        // 2) 검색된 조문만 근거로 답변
        String answer = chatClient.prompt()
                .system("""
                        당신은 청소년복지 지원법 안내 도우미입니다.
                        반드시 아래 [참고 조문]의 내용만 근거로 한국어로 간결하게 답하세요.
                        답변에 근거 조문 번호를 함께 적으세요.
                        참고 조문에 없는 내용은 '제공된 법령에서 확인할 수 없습니다'라고 답하세요.
                        """)
                .user("[참고 조문]\n" + context + "\n\n[질문]\n" + q)
                .call()
                .content();

        String sources = hits.stream()
                .map(d -> String.valueOf(d.getMetadata().get("article")))
                .collect(Collectors.joining(", "));

        return answer + "\n\n[검색된 조문] " + sources;
    }
}