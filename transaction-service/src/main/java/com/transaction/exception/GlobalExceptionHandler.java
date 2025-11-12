package com.transaction.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ProblemDetail base(HttpStatus status, String title, String detail, String type, HttpServletRequest req) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        if (type != null) p.setType(URI.create(type));
        if (req != null) p.setInstance(URI.create(req.getRequestURI()));
        String traceId = org.slf4j.MDC.get("traceId");
        if (traceId != null) p.setProperty("traceId", traceId);
        p.setProperty("timestamp", OffsetDateTime.now().toString());
        return p;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail p = base(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields are invalid.", "https://errors.yourco.com/validation-error", req);
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream().collect(java.util.stream.Collectors.toMap(
                        e -> e.getField(), e -> e.getDefaultMessage(), (a,b)->a));
        p.setProperty("errors", fieldErrors);
        return p;
    }

    // 502/503 when a downstream fails (gateway style)
    @ExceptionHandler(DownstreamUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleDownstreamUnavailable(DownstreamUnavailableException ex, HttpServletRequest req) {
        ProblemDetail p = base(HttpStatus.BAD_GATEWAY, "Downstream Unavailable",
                ex.getMessage(), "https://errors.yourco.com/downstream-unavailable", req);
        p.setProperty("downstream", ex.getService());
        p.setProperty("endpoint", ex.getEndpoint());
        p.setProperty("downstreamStatus", ex.getStatus());
        p.setProperty("downstreamBody", truncate(ex.getBody(), 1000));
        return p;
    }

    @ExceptionHandler(DownstreamAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleDownstreamDenied(DownstreamAccessDeniedException ex, HttpServletRequest req) {
        ProblemDetail p = base(HttpStatus.FORBIDDEN, "Downstream Access Denied",
                "The downstream service rejected the call (403).", "https://errors.yourco.com/downstream-access-denied", req);
        p.setProperty("downstream", ex.getService());
        p.setProperty("endpoint", ex.getEndpoint());
        p.setProperty("downstreamStatus", ex.getStatus());
        return p;
    }

    @ExceptionHandler(DownstreamBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleDownstreamBadRequest(DownstreamBadRequestException ex, HttpServletRequest req) {
        // We still surface it as 502 because *our* API was valid, but the downstream rejected input.
        ProblemDetail p = base(HttpStatus.BAD_GATEWAY, "Downstream Rejected Request",
                "Downstream rejected the request.", "https://errors.yourco.com/downstream-bad-request", req);
        p.setProperty("downstream", ex.getService());
        p.setProperty("endpoint", ex.getEndpoint());
        p.setProperty("downstreamStatus", ex.getStatus());
        p.setProperty("downstreamBody", truncate(ex.getBody(), 1000));
        return p;
    }

    @ExceptionHandler(DownstreamNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleDownstreamNotFound(DownstreamNotFoundException ex, HttpServletRequest req) {
        ProblemDetail p = base(HttpStatus.BAD_GATEWAY, "Downstream Not Found",
                "Resource not found in downstream service.", "https://errors.yourco.com/downstream-not-found", req);
        p.setProperty("downstream", ex.getService());
        p.setProperty("endpoint", ex.getEndpoint());
        p.setProperty("downstreamStatus", ex.getStatus());
        return p;
    }

    @ExceptionHandler(DownstreamServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleDownstreamGeneric(DownstreamServiceException ex, HttpServletRequest req) {
        ProblemDetail p = base(HttpStatus.BAD_GATEWAY, "Downstream Error",
                ex.getMessage(), "https://errors.yourco.com/downstream-error", req);
        p.setProperty("downstream", ex.getService());
        p.setProperty("endpoint", ex.getEndpoint());
        p.setProperty("downstreamStatus", ex.getStatus());
        p.setProperty("downstreamBody", truncate(ex.getBody(), 1000));
        return p;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        return base(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred.", "https://errors.yourco.com/internal", req);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
