package com.kyc.service;

import com.kyc.dto.KycRequest;
import com.kyc.entity.KycDocument;
import com.kyc.repository.KycRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycServiceImpl {

    private final KycRepository kycRepository;
    private final VaultEncryptionService encryptionService;

    public KycDocument submitKyc(KycRequest request) {
        log.info("Processing KYC submission for customer: {}", request.getCustomerId());

        KycDocument document = KycDocument.builder()
                .customerId(request.getCustomerId())
                .aadhaarEnc(encryptionService.encrypt(request.getAadhaar(), request.getCustomerId(), "AADHAAR"))
                .panEnc(encryptionService.encrypt(request.getPan(), request.getCustomerId(), "PAN"))
                .passportEnc(encryptionService.encrypt(request.getPassport(), request.getCustomerId(), "PASSPORT"))
                .aadhaarMasked(mask(request.getAadhaar()))
                .panMasked(mask(request.getPan()))
                .passportMasked(mask(request.getPassport()))
                .status("PENDING")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return kycRepository.save(document);
    }

    public KycDocument getKyc(String customerId) {
        return kycRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("KYC not found for customer: " + customerId));
    }

    public KycRequest decryptKyc(String customerId, String userId) {
        KycDocument doc = getKyc(customerId);
        KycRequest request = new KycRequest();
        request.setCustomerId(doc.getCustomerId());
        request.setAadhaar(encryptionService.decrypt(doc.getAadhaarEnc(), userId, customerId, "AADHAAR"));
        request.setPan(encryptionService.decrypt(doc.getPanEnc(), userId, customerId, "PAN"));
        request.setPassport(encryptionService.decrypt(doc.getPassportEnc(), userId, customerId, "PASSPORT"));
        return request;
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }
}
