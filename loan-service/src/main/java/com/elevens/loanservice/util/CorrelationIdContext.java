package com.elevens.loanservice.util;

public class CorrelationIdContext {

    public static final String CORRELATION_ID = "eleven-correlation-id";

    private static final ThreadLocal<String> correlationId = new ThreadLocal<String>();

    public static String getCorrelationId() {
        return correlationId.get();
    }

    public static void setCorrelationId(String cid) {
        correlationId.set(cid);
    }

    public static void clear() {
        correlationId.remove();
    }
}
