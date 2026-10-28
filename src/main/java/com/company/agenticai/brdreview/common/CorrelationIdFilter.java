
package com.company.agenticai.brdreview.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String HEADER_NAME = "X-Correlation-ID";

    private static final long MAX_REQUEST_SIZE = 1_048_576L; // 1 MB

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(HEADER_NAME);

        if (correlationId == null
                || !correlationId.matches("[a-zA-Z0-9._-]{1,64}")) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        long startTime = System.nanoTime();

        try {
            long contentLength = request.getContentLengthLong();

            if (contentLength > MAX_REQUEST_SIZE) {
                response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                response.getWriter().write("""
                        {
                          "status": 413,
                          "error": "Content Too Large",
                          "message": "The request body exceeds the maximum allowed size."
                        }
                        """);

                return;
            }

            filterChain.doFilter(request, response);

        } finally {
            long durationMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            logger.info(
                    "HTTP request completed: method={}, path={}, status={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );

            MDC.remove("correlationId");
        }
    }
}
