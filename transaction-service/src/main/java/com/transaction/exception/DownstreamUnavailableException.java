package com.transaction.exception;

public class DownstreamUnavailableException extends DownstreamServiceException {
    public DownstreamUnavailableException(String svc, String ep, int st, String body) {
        super(svc, ep, st, body, "Downstream service unavailable");
    }
}
