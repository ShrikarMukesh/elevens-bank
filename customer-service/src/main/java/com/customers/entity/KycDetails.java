package com.customers.entity;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycDetails {
    private String aadhaar;
    private String pan;
    private String passport;
    private boolean verified;     // ✅ Added
    private Instant verifiedAt;   // ✅ Added
}
