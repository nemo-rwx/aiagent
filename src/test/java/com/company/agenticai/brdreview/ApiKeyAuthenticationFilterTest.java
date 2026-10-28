
package com.company.agenticai.brdreview.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyAuthenticationFilterTest {

    private static final String VALID_KEY =
            "test-secret-key-for-unit-tests";

    @Test
    void shouldRejectRequestWithMissingApiKey()
            throws ServletException, IOException {

        ApiKeyAuthenticationFilter filter =
                new ApiKeyAuthenticationFilter(true, VALID_KEY);

        MockHttpServletResponse response =
                executeFilter(filter, null, false);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString()
                .contains("A valid API key is required."));
        assertTrue(response.containsHeader("WWW-Authenticate"));
    }

    @Test
    void shouldRejectRequestWithInvalidApiKey()
            throws ServletException, IOException {

        ApiKeyAuthenticationFilter filter =
                new ApiKeyAuthenticationFilter(true, VALID_KEY);

        MockHttpServletResponse response =
                executeFilter(filter, "incorrect-key", false);

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldAllowRequestWithValidApiKey()
            throws ServletException, IOException {

        ApiKeyAuthenticationFilter filter =
                new ApiKeyAuthenticationFilter(true, VALID_KEY);

        boolean[] chainReached = {false};

        MockHttpServletRequest request = createRequest();
        request.addHeader("X-API-Key", VALID_KEY);

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = (req, res) -> chainReached[0] = true;

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chainReached[0],
                "Valid requests should continue through the filter chain");
    }

    @Test
    void shouldFailClosedWhenApiKeyIsNotConfigured()
            throws ServletException, IOException {

        ApiKeyAuthenticationFilter filter =
                new ApiKeyAuthenticationFilter(true, "");

        MockHttpServletResponse response =
                executeFilter(filter, VALID_KEY, false);

        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString()
                .contains("API authentication is not configured."));
    }

    @Test
    void shouldAllowRequestsWhenAuthenticationIsDisabled()
            throws ServletException, IOException {

        ApiKeyAuthenticationFilter filter =
                new ApiKeyAuthenticationFilter(false, "");

        boolean[] chainReached = {false};

        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain = (req, res) -> chainReached[0] = true;

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chainReached[0]);
    }

    private MockHttpServletResponse executeFilter(
            ApiKeyAuthenticationFilter filter,
            String apiKey,
            boolean expectedToReachChain)
            throws ServletException, IOException {

        MockHttpServletRequest request = createRequest();

        if (apiKey != null) {
            request.addHeader("X-API-Key", apiKey);
        }

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        boolean[] chainReached = {false};

        FilterChain chain = (req, res) -> chainReached[0] = true;

        filter.doFilter(request, response, chain);

        assertEquals(expectedToReachChain, chainReached[0]);

        return response;
    }

    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setMethod("POST");
        request.setRequestURI("/api/v1/brd/review");

        return request;
    }
}
