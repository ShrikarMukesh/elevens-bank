package com.transaction.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import feign.Util;

import java.nio.charset.StandardCharsets;

public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        String body = "";
        try {
            if (response.body() != null) {
                body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}

        int status = response.status();
        String url = response.request() != null ? response.request().url() : "";
        String service = "account-service"; // put your client name here

        // Map common statuses to typed exceptions
        switch (status) {
            case 400 -> { return new DownstreamBadRequestException(service, url, status, body); }
            case 401, 403 -> { return new DownstreamAccessDeniedException(service, url, status, body); }
            case 404 -> { return new DownstreamNotFoundException(service, url, status, body); }
            case 409, 422 -> { return new DownstreamServiceException(service, url, status, body, "Downstream conflict or business error"); }
            case 502, 503, 504 -> { return new DownstreamUnavailableException(service, url, status, body); }
            default -> { return new DownstreamServiceException(service, url, status, body, "Downstream error"); }
        }
    }
}
