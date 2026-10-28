
package com.company.agenticai.brdreview.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private static final String REVIEW_ENDPOINT =
            "/api/v1/brd/review";

    private static final String HISTORY_ENDPOINT_PREFIX =
            "/api/v1/brd/reviews/";

    private final boolean enabled;
    private final String configuredApiKey;

    public ApiKeyAuthenticationFilter(
            @Value("${api.security.enabled:true}") boolean enabled,
            @Value("${api.security.key:}") String configuredApiKey) {

        this.enabled = enabled;
        this.configuredApiKey = configuredApiKey;

        // Never print the actual API key.
        System.out.println(
                "API authentication enabled: " + enabled
                        + ", API key configured: "
                        + (configuredApiKey != null
                        && !configuredApiKey.isBlank())
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String method = request.getMethod();
        String requestUri = request.getRequestURI();

        boolean reviewRequest =
                "POST".equalsIgnoreCase(method)
                        && REVIEW_ENDPOINT.equals(requestUri);

        boolean historyRequest =
                "GET".equalsIgnoreCase(method)
                        && requestUri.startsWith(HISTORY_ENDPOINT_PREFIX);

        return !reviewRequest && !historyRequest;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Fail closed when the server has no configured API key.
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            writeError(
                    response,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Service Unavailable",
                    "API authentication is not configured."
            );
            return;
        }

        String suppliedApiKey = request.getHeader(API_KEY_HEADER);

        if (suppliedApiKey == null
                || suppliedApiKey.isBlank()
                || !MessageDigest.isEqual(
                configuredApiKey.getBytes(StandardCharsets.UTF_8),
                suppliedApiKey.getBytes(StandardCharsets.UTF_8))) {

            response.setHeader("WWW-Authenticate", "ApiKey");

            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "A valid API key is required."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String error,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write("""
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s"
                }
                """.formatted(status, error, message));
    }
}
