package com.kyc.controller;

import com.kyc.dto.KycRequest;
import com.kyc.entity.KycDocument;
import com.kyc.service.KycServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycServiceImpl kycService;

    @PostMapping
    public ResponseEntity<KycDocument> submitKyc(@RequestBody KycRequest request) {
        return ResponseEntity.ok(kycService.submitKyc(request));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<KycDocument> getKyc(@PathVariable String customerId) {
        return ResponseEntity.ok(kycService.getKyc(customerId));
    }
}
