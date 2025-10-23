package com.cards.repository;

import com.cards.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByCustomerId(Long customerId);
    List<Card> findByAccountId(Long accountId);
    Optional<Card> findByCardNumber(String cardNumber);
}
