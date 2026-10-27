
package com.company.agenticai.brdreview.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException exception) {

        logger.warn("Invalid request received.");

        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", "The request is invalid. Check the request body and required fields."
        ));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            Exception exception) {

        logger.warn("Request validation failed: {}",
                exception.getClass().getSimpleName());

        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", "The request is invalid. Check the request body and required fields."
        ));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleConnectionFailure(
            ResourceAccessException exception) {

        if (hasTimeoutCause(exception)) {
            logger.warn("AI provider request timed out.");

            return errorResponse(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "The AI service took too long to respond. Please try again."
            );
        }

        logger.warn("AI provider connection failed: {}",
                exception.getClass().getSimpleName());

        return errorResponse(
                HttpStatus.BAD_GATEWAY,
                "The AI service is currently unreachable. Please try again later."
        );
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<Map<String, Object>> handleProviderHttpError(
            HttpStatusCodeException exception) {

        int providerStatus = exception.getStatusCode().value();

        logger.warn("AI provider returned HTTP status: {}", providerStatus);

        if (providerStatus == 429) {
            return errorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI service is busy. Please try again later."
            );
        }

        return errorResponse(
                HttpStatus.BAD_GATEWAY,
                "The AI service could not complete the request."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedError(
            Exception exception) {

        logger.error("Unexpected error while processing request: {}",
                exception.getClass().getSimpleName());

        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The request could not be completed. Please try again later."
        );
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status,
            String message) {

        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message
        ));
    }
}
