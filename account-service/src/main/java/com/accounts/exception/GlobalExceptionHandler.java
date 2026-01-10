package com.accounts.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private ProblemDetail pd(HttpStatus status, String title, String detail,
                             String type, HttpServletRequest req) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        if (type != null) p.setType(URI.create(type));
        if (req != null) {
            p.setInstance(URI.create(req.getRequestURI()));
        }
        // enrich with context (traceId from MDC if using Observability/Brave)
        String traceId = org.slf4j.MDC.get("traceId");
        if (traceId != null) p.setProperty("traceId", traceId);
        p.setProperty("timestamp", OffsetDateTime.now().toString());
        return p;
    }

    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDenied(Exception ex, HttpServletRequest req) {
        log.warn("Access denied: {}", ex.getMessage());
        return pd(HttpStatus.FORBIDDEN, "Access Denied",
                "You are not allowed to perform this operation.",
                "https://errors.yourco.com/access-denied", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        log.warn("Validation failed: {}", ex.getMessage());
        ProblemDetail p = pd(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields are invalid.",
                "https://errors.yourco.com/validation-error", req);
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream().collect(java.util.stream.Collectors.toMap(
                        e -> e.getField(), e -> e.getDefaultMessage(), (a,b) -> a));
        p.setProperty("errors", fieldErrors);
        return p;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("Resource not found: {}", ex.getMessage());
        return pd(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(),
                "https://errors.yourco.com/not-found", req);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ProblemDetail handleBusiness(InsufficientBalanceException ex, HttpServletRequest req) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return pd(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violated", ex.getMessage(),
                "https://errors.yourco.com/business-rule", req);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected internal server error", ex);
        return pd(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                ex.getMessage(), // Exposing message for debugging
                "https://errors.yourco.com/internal", req);
    }
}
