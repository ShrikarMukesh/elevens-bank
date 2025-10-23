package com.cards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableMethodSecurity
public class CardsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardsServiceApplication.class, args);
    }
}