package com.transaction.exception;

public class DownstreamServiceException extends RuntimeException {
    private final String service;
    private final String endpoint;
    private final int status;
    private final String body;

    public DownstreamServiceException(String service, String endpoint, int status, String body, String message) {
        super(message);
        this.service = service;
        this.endpoint = endpoint;
        this.status = status;
        this.body = body;
    }
    public String getService() { return service; }
    public String getEndpoint() { return endpoint; }
    public int getStatus() { return status; }
    public String getBody() { return body; }
}




