
package com.company.agenticai.brdreview.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, ClientWindow> clients =
            new ConcurrentHashMap<>();

    private final int maxRequests;
    private final long windowMillis;

    public RateLimitFilter(
            @Value("${rate-limit.max-requests:10}") int maxRequests,
            @Value("${rate-limit.window-millis:60000}") long windowMillis) {

        if (maxRequests < 1 || windowMillis < 1) {
            throw new IllegalArgumentException(
                    "Rate-limit settings must be positive."
            );
        }

        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/brd/review".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        ClientWindow window = clients.compute(clientIp, (key, existing) -> {
            if (existing == null
                    || now - existing.startedAt >= windowMillis) {
                return new ClientWindow(now);
            }

            return existing;
        });

        int requestCount = window.count.incrementAndGet();

        if (requestCount > maxRequests) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader(
                    "Retry-After",
                    String.valueOf(Math.max(
                            1,
                            (window.startedAt + windowMillis - now + 999) / 1000
                    ))
            );

            response.getWriter().write("""
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "message": "Too many review requests. Please try again later."
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Scheduled(fixedDelayString = "${rate-limit.cleanup-interval-millis:60000}")
    public void removeExpiredWindows() {
        long now = System.currentTimeMillis();

        clients.entrySet().removeIf(entry ->
                now - entry.getValue().startedAt >= windowMillis
        );
    }

    private static final class ClientWindow {
        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger();

        private ClientWindow(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
