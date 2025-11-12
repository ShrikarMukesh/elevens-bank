package com.transaction.exception;

public class DownstreamAccessDeniedException extends DownstreamServiceException {
    public DownstreamAccessDeniedException(String svc, String ep, int st, String body) {
        super(svc, ep, st, body, "Downstream access denied");
    }
}