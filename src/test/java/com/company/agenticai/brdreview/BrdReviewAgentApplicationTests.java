
package com.company.agenticai.brdreview;

import com.company.agenticai.brdreview.dto.BrdReviewRequest;
import com.company.agenticai.brdreview.dto.BrdReviewResponse;
import com.company.agenticai.brdreview.service.BrdReviewService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"openrouter.base-url=https://openrouter.ai/api/v1",
		"openrouter.model=openai/gpt-4o-mini",
		"api.security.enabled=false"
})
class BrdReviewAgentApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BrdReviewService brdReviewService;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldReturnSuccessfulReview() throws Exception {
		BrdReviewResponse response = new BrdReviewResponse(
				"SUCCESS",
				"Review completed.",
				90,
				List.of(),
				List.of("What happens if payment processing fails?")
		);

		when(brdReviewService.reviewBrd(any(BrdReviewRequest.class)))
				.thenReturn(response);

		mockMvc.perform(post("/api/v1/brd/review")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                                {
                                  "brdText": "The system shall process payments."
                                }
                                """))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCESS"))
				.andExpect(jsonPath("$.completenessScore").value(90));
	}

	@Test
	void shouldRejectBlankBrd() throws Exception {
		mockMvc.perform(post("/api/v1/brd/review")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                                {
                                  "brdText": ""
                                }
                                """))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void shouldRejectMissingBrdText() throws Exception {
		mockMvc.perform(post("/api/v1/brd/review")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void shouldRejectMalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/brd/review")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                                {
                                  "brdText": "Incomplete JSON"
                                """))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").exists());
	}


	@Test
	void shouldRejectRequestLargerThanOneMegabyte() throws Exception {
		String oversizedBrd = "A".repeat(1_100_000);

		String requestBody = new com.fasterxml.jackson.databind.ObjectMapper()
				.writeValueAsString(
						new BrdReviewRequest(oversizedBrd)
				);

		mockMvc.perform(post("/api/v1/brd/review")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().is(413))
				.andExpect(jsonPath("$.status").value(413))
				.andExpect(jsonPath("$.error").value("Content Too Large"));
	}

}
