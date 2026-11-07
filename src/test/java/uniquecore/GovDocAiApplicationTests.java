package uniquecore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GovDocAiApplicationTests {

	@Disabled("Ollama와 pgvector가 필요한 통합 테스트 - 로컬에서만 실행")
	@Test
	void contextLoads() {
	}
}