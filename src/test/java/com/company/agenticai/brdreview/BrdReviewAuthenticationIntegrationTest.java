
package com.company.agenticai.brdreview;

import com.company.agenticai.brdreview.service.BrdReviewService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.company.agenticai.brdreview.dto.BrdReviewResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "api.security.enabled=true",
        "api.security.key=integration-test-secret",
        "rate-limit.max-requests=100",
        "rate-limit.window-millis=60000"
})
class BrdReviewAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrdReviewService brdReviewService;

    private static final String REQUEST_BODY = """
            {
              "brdText": "The system shall process payments."
            }
            """;

    @Test
    void shouldRejectRequestWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/brd/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldRejectRequestWithInvalidApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/brd/review")
                        .header("X-API-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }


    @Test
    void shouldAllowRequestWithValidApiKey() throws Exception {
        BrdReviewResponse response = new BrdReviewResponse(
                "SUCCESS",
                "Review completed.",
                90,
                List.of(),
                List.of()
        );

        when(brdReviewService.reviewBrd(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/brd/review")
                        .header("X-API-Key", "integration-test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.completenessScore").value(90));
    }

}
