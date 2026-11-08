package uniquecore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class LawArticleSplitterTest {

    @Test
    void 조문_단위로_분할하고_조문번호를_메타데이터에_넣는다() {
        String text = """
                제1조(목적) 이 법은 청소년복지 향상에 관한 사항을 규정함을 목적으로 한다.
                제2조(정의) 이 법에서 사용하는 용어의 뜻은 다음과 같다.
                제9조의2(특별지원) 국가는 위기청소년에게 필요한 지원을 할 수 있다.
                """;

        List<Document> docs = LawArticleSplitter.split(text, "청소년복지 지원법");

        assertThat(docs).hasSize(3);
        assertThat(docs).extracting(d -> d.getMetadata().get("article"))
                .containsExactly("제1조", "제2조", "제9조의2");
    }

    @Test
    void 너무_짧은_조각은_버린다() {
        List<Document> docs = LawArticleSplitter.split("목차\n제1조(목적) 짧음", "test");

        assertThat(docs).isEmpty();
    }

        @Test
    void 긴_조문은_여러_조각으로_나누고_모든_조각에_조문_제목을_붙인다() {
        StringBuilder sb = new StringBuilder("제2조(정의) 이 법에서 사용하는 용어의 뜻은 다음과 같다.\n");
        for (int i = 1; i <= 8; i++) {
            sb.append(i).append(". \"용어").append(i).append("\"란 ")
              .append("설명 문장이 길게 이어지는 정의 내용입니다. ".repeat(3)).append("\n");
        }

        List<Document> docs = LawArticleSplitter.split(sb.toString(), "test");

        assertThat(docs.size()).isGreaterThan(1);
        assertThat(docs).allSatisfy(d -> {
            assertThat(d.getMetadata().get("article")).isEqualTo("제2조");
            assertThat(d.getText()).startsWith("제2조(정의)");
        });
    }
}