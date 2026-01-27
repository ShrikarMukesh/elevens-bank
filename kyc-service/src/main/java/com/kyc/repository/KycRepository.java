package com.kyc.repository;

import com.kyc.entity.KycDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KycRepository extends MongoRepository<KycDocument, String> {
    Optional<KycDocument> findByCustomerId(String customerId);
}
