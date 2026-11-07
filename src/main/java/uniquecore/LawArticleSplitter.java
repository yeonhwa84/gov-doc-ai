package uniquecore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;

/** 법령 본문을 "제N조(...)" 단위로 잘라 Document 목록으로 만든다. */
public final class LawArticleSplitter {

    private LawArticleSplitter() {}

    public static List<Document> split(String text, String source) {
        String[] parts = text.split("(?=제\\d+조(?:의\\d+)?\\()");

        List<Document> docs = new ArrayList<>();
        for (String part : parts) {
            String body = part.strip();
            if (body.length() < 20) continue;

            int idx = body.indexOf('(');
            String article = idx > 0 ? body.substring(0, idx) : "기타";

            docs.add(new Document(body, Map.of("source", source, "article", article)));
        }
        return docs;
    }
}