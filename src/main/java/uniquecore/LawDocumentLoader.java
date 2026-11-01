package uniquecore;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class LawDocumentLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LawDocumentLoader.class);
    private static final File STORE_FILE = new File("data/vector-store.json");

    private final SimpleVectorStore vectorStore;

    public LawDocumentLoader(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 이미 임베딩해 둔 파일이 있으면 불러오기만 함
        if (STORE_FILE.exists()) {
            vectorStore.load(STORE_FILE);
            log.info("벡터 저장소 파일 로드 완료: {}", STORE_FILE.getPath());
            return;
        }

        String text = new ClassPathResource("docs/youth-welfare-act.txt")
                .getContentAsString(StandardCharsets.UTF_8);

        // "제1조(목적)", "제9조의2(...)" 앞에서 잘라 조문 단위로 분리
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

        STORE_FILE.getParentFile().mkdirs();
        vectorStore.save(STORE_FILE);
        log.info("임베딩 완료 및 저장: {}", STORE_FILE.getPath());
    }
}
