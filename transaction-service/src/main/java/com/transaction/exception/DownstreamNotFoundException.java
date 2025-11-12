package com.transaction.exception;

public class DownstreamNotFoundException extends DownstreamServiceException {
    public DownstreamNotFoundException(String svc, String ep, int st, String body) {
        super(svc, ep, st, body, "Downstream resource not found");
    }
}