package com.transaction.exception;

public class DownstreamBadRequestException extends DownstreamServiceException {
    public DownstreamBadRequestException(String svc, String ep, int st, String body) {
        super(svc, ep, st, body, "Downstream bad request");
    }
}