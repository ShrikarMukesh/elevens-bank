package com.customers.entity;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycDetails {
    private String kycId; // Reference to KYC Service Document ID
    private String status; // PENDING, VERIFIED, REJECTED
    private Instant verifiedAt;
}
