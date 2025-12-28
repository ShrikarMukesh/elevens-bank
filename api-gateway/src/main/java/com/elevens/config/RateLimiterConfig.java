package com.elevens.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Configuration class for Rate Limiting in the API Gateway.
 * <p>
 * This class defines how we identify "who" is making a request so that we can
 * count
 * their requests and limit them if they exceed the allowed quota.
 * </p>
 */
@Configuration
public class RateLimiterConfig {

        /**
         * Defines the KeyResolver bean that determines the unique key for limiting
         * requests.
         * <p>
         * <b>Interview Concept: Key Resolution</b><br>
         * In Rate Limiting (e.g., Token Bucket algorithm), we need a way to group
         * requests.
         * Common strategies include:
         * <ul>
         * <li><b>Per User:</b> Use the User ID (from JWT/Session). Good for logged-in
         * users.</li>
         * <li><b>Per IP:</b> Use the Client's IP Address. Good for public APIs (DDOS
         * protection).</li>
         * <li><b>Global:</b> One limit for the whole system (rarely used).</li>
         * </ul>
         * <p>
         * <b>Implementation Details:</b><br>
         * Only Reactive types (Mono) are supported here because Spring Cloud Gateway is
         * built on project Reactor (Non-blocking I/O).
         *
         * @return A KeyResolver that extracts the Client's IP Address.
         */
        @Bean
        public KeyResolver userKeyResolver() {
                return exchange -> {
                        // "exchange" is the ServerWebExchange (similar to HttpServletRequest but for
                        // WebFlux/Netty).
                        // It holds the request and response context.

                        return Mono.just(
                                        Objects.requireNonNull(exchange.getRequest().getRemoteAddress()) // Get network
                                                                                                         // info
                                                        .getAddress() // Get InetAddress
                                                        .getHostAddress() // Extract String IP (e.g., "192.168.1.5")
                        );
                };
        }
}
