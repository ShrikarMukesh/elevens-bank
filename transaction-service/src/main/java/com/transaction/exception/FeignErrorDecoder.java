package com.transaction.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 400 -> new RuntimeException("Bad Request from Account Service");
            case 404 -> new RuntimeException("Account not found");
            case 500 -> new RuntimeException("Account Service error");
            default -> new RuntimeException("Unexpected error: " + response.status());
        };
    }
}

