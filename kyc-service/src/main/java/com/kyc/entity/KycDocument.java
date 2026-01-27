package com.kyc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "kyc_documents")
public class KycDocument {

    @Id
    private String id;

    private String customerId; // Linked to Customer Service

    // Encrypted Fields (Stored as Ciphertext)
    private String aadhaarEnc;
    private String panEnc;
    private String passportEnc;

    // Masked Fields (Safe to view)
    private String aadhaarMasked;
    private String panMasked;
    private String passportMasked;

    private String status; // PENDING, VERIFIED, REJECTED
    
    private Instant createdAt;
    private Instant updatedAt;
}
