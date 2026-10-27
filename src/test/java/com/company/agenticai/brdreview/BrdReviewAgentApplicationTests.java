
package com.company.agenticai.brdreview;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"openrouter.base-url=https://openrouter.ai/api/v1",
		"openrouter.model=openai/gpt-4o-mini"
})
class BrdReviewAgentApplicationTests {

	@Test
	void contextLoads() {
	}
}
