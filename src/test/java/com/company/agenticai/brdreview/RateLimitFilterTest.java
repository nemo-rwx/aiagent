
package com.company.agenticai.brdreview.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    @Test
    void shouldRejectRequestsExceedingRateLimit()
            throws ServletException, IOException {

        RateLimitFilter filter = new RateLimitFilter(3, 60_000);

        for (int i = 0; i < 4; i++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest();
            request.setMethod("POST");
            request.setRequestURI("/api/v1/brd/review");
            request.setRemoteAddr("192.0.2.10");

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            FilterChain chain = (req, res) -> {
                // The first three requests should reach this chain.
            };

            filter.doFilter(request, response, chain);

            if (i < 3) {
                assertEquals(200, response.getStatus(),
                        "Request " + (i + 1) + " should be allowed");
            } else {
                assertEquals(429, response.getStatus());
                assertTrue(response.containsHeader("Retry-After"));
                assertTrue(response.getContentAsString()
                        .contains("Too Many Requests"));
            }
        }
    }
}
