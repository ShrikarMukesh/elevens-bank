package com.kyc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;

@Service
@RequiredArgsConstructor
@Slf4j
public class VaultEncryptionService {

    private final VaultOperations vaultOperations;
    private final AuditService auditService;
    private static final String KEY_NAME = "kyc-pii";

    public String encrypt(String plaintextValue, String resourceId, String field) {
        if (plaintextValue == null || plaintextValue.isEmpty()) {
            return null;
        }
        try {
            Ciphertext ciphertext = vaultOperations.opsForTransit().encrypt(KEY_NAME, Plaintext.of(plaintextValue));
            // Optional: Log encryption too, but decrypt is more critical
            return ciphertext.getCiphertext();
        } catch (Exception e) {
            log.error("Encryption failed for key: {}", KEY_NAME, e);
            auditService.logEvent("SYSTEM", resourceId, "ENCRYPT", field, "FAILURE", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertextValue, String userId, String resourceId, String field) {
        if (ciphertextValue == null || ciphertextValue.isEmpty()) {
            return null;
        }
        try {
            Plaintext plaintext = vaultOperations.opsForTransit().decrypt(KEY_NAME, Ciphertext.of(ciphertextValue));
            auditService.logEvent(userId, resourceId, "DECRYPT", field, "SUCCESS", "Authorized Access");
            return plaintext.asString();
        } catch (Exception e) {
            log.error("Decryption failed for key: {}", KEY_NAME, e);
            auditService.logEvent(userId, resourceId, "DECRYPT", field, "FAILURE", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
