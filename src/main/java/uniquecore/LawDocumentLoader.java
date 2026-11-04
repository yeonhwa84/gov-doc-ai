package uniquecore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LawDocumentLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LawDocumentLoader.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public LawDocumentLoader(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 이미 DB에 임베딩이 있으면 건너뜀
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store", Integer.class);
        if (count != null && count > 0) {
            log.info("vector_store에 이미 {}건 있음 → 임베딩 생략", count);
            return;
        }

        String text = new ClassPathResource("docs/youth-welfare-act.txt")
                .getContentAsString(StandardCharsets.UTF_8);

        String[] parts = text.split("(?=제\\d+조(?:의\\d+)?\\()");

        List<Document> docs = new ArrayList<>();
        for (String part : parts) {
            String body = part.strip();
            if (body.length() < 20) continue;

            int idx = body.indexOf('(');
            String article = idx > 0 ? body.substring(0, idx) : "기타";

            docs.add(new Document(body, Map.of(
                    "source", "청소년복지 지원법",
                    "article", article)));
        }

        log.info("조문 {}개 임베딩 시작 (CPU에서는 몇 분 걸릴 수 있음)", docs.size());
        vectorStore.add(docs);
        log.info("임베딩 완료 → PostgreSQL vector_store에 저장");
    }
}