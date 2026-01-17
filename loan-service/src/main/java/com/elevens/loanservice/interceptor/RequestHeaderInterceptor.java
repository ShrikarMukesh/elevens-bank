package com.elevens.loanservice.interceptor;

import com.elevens.loanservice.util.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestHeaderInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestHeaderInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String correlationId = request.getHeader(CorrelationIdContext.CORRELATION_ID);
        if (correlationId != null) {
            CorrelationIdContext.setCorrelationId(correlationId);
            logger.debug("eleven-correlation-id found in RequestHeaderInterceptor: {}", correlationId);
        } else {
            logger.debug("eleven-correlation-id not found in RequestHeaderInterceptor");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        CorrelationIdContext.clear();
    }
}
