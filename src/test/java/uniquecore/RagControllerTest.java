package uniquecore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RagControllerTest {

    @Test
    void 기준값_이상인_조문이_없으면_AI를_호출하지_않고_안내문을_반환한다() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);

        RagController controller = new RagController(builder, vectorStore, 0.5);

        String result = controller.rag("청소년 아르바이트 최저임금은?");

        assertThat(result).contains("제공된 법령에서 확인할 수 없습니다");
        verifyNoInteractions(chatClient);   // AI 호출이 한 번도 없어야 함
    }
}