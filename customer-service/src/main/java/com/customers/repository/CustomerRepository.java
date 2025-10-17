package com.customers.repository;

import com.customers.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {

    Optional<Customer> findByCustomerId(String customerId);

    Optional<Customer> findByUserId(String userId);

    Optional<Customer> findByEmail(String email);

    List<Customer> findByStatus(String status);

    boolean existsByCustomerId(String customerId);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);
}
