package com.kyc.dto;

import lombok.Data;

@Data
public class KycRequest {
    private String customerId;
    private String aadhaar;
    private String pan;
    private String passport;
}
