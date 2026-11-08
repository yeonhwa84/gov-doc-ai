package uniquecore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;

/** 법령 본문을 조문 단위로 자르고, 긴 조문은 항·호 단위로 더 나눈다. */
public final class LawArticleSplitter {

    /** 이 길이를 넘는 조문은 더 잘게 나눈다 (글자 수) */
    static final int MAX_LEN = 300;

    private LawArticleSplitter() {}

    public static List<Document> split(String text, String source) {
        String[] parts = text.split("(?=제\\d+조(?:의\\d+)?\\()");

        List<Document> docs = new ArrayList<>();
        for (String part : parts) {
            String body = part.strip();
            if (body.length() < 20) continue;

            int open = body.indexOf('(');
            String article = open > 0 ? body.substring(0, open) : "기타";

            if (body.length() <= MAX_LEN) {
                docs.add(doc(body, source, article));
                continue;
            }

            // 긴 조문: 제목 "제2조(정의)"를 떼어 두고, 항(①②…)·호(1. 2. …) 앞에서 자른다
            int close = body.indexOf(')');
            String title = close > 0 ? body.substring(0, close + 1) : article;
            String rest = body.substring(title.length());
            String[] pieces = rest.split("(?=[①-⑳])|(?=\\n\\s*\\d+\\.\\s)");

            // 조각이 너무 잘게 쪼개지지 않도록 MAX_LEN 안에서 이어 붙인다
            StringBuilder buf = new StringBuilder();
            for (String piece : pieces) {
                if (buf.length() > 0 && buf.length() + piece.length() > MAX_LEN) {
                    docs.add(doc(title + " " + buf.toString().strip(), source, article));
                    buf.setLength(0);
                }
                buf.append(piece);
            }
            if (!buf.toString().isBlank()) {
                docs.add(doc(title + " " + buf.toString().strip(), source, article));
            }
        }
        return docs;
    }

    private static Document doc(String text, String source, String article) {
        return new Document(text, Map.of("source", source, "article", article));
    }
}