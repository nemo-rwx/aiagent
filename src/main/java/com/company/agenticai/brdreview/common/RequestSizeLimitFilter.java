
package com.company.agenticai.brdreview.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUEST_SIZE = 1_048_576;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long declaredLength = request.getContentLengthLong();

        if (declaredLength > MAX_REQUEST_SIZE) {
            writeTooLarge(response);
            return;
        }

        // Only buffer JSON requests. The controller accepts JSON.
        if (request.getContentType() == null
                || !request.getContentType().toLowerCase()
                .startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] body;

        try (InputStream input = request.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int total = 0;
            int read;

            while ((read = input.read(buffer)) != -1) {
                total += read;

                if (total > MAX_REQUEST_SIZE) {
                    writeTooLarge(response);
                    return;
                }

                output.write(buffer, 0, read);
            }

            body = output.toByteArray();
        }

        filterChain.doFilter(
                new CachedBodyRequestWrapper(request, body),
                response
        );
    }

    private void writeTooLarge(HttpServletResponse response)
            throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.resetBuffer();
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
    }

    private static class CachedBodyRequestWrapper
            extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(
                HttpServletRequest request,
                byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {

                private final java.io.ByteArrayInputStream input =
                        new java.io.ByteArrayInputStream(body);

                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException(
                            "Asynchronous reads are not supported."
                    );
                }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                            getInputStream(),
                            getCharacterEncoding() == null
                                    ? java.nio.charset.StandardCharsets.UTF_8
                                    : java.nio.charset.Charset.forName(
                                    getCharacterEncoding()
                            )
                    )
            );
        }
    }
}
