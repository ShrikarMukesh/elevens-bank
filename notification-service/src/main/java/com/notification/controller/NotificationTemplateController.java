package com.notification.controller;

import com.notification.entity.NotificationTemplate;
import com.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateController {

    private final NotificationTemplateRepository templateRepository;

    @GetMapping("/status")
    public Mono<String> status() {
        log.info("GET /api/templates/status - checking service status");
        return Mono.just("Notification service is up and running");
    }

    @GetMapping
    public Flux<NotificationTemplate> getAll() {
        log.info("GET /api/templates - fetching all templates");
        return templateRepository.findAll()
                .doOnComplete(() -> log.info("Finished fetching all templates"));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<NotificationTemplate>> getById(@PathVariable String id) {
        log.info("GET /api/templates/{} - fetching template by ID", id);
        return templateRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Found template with ID {}", id);
                    } else {
                        log.warn("Template with ID {} not found", id);
                    }
                });
    }

    @PostMapping
    public Mono<NotificationTemplate> create(@RequestBody NotificationTemplate template) {
        log.info("POST /api/templates - creating new template");
        return templateRepository.save(template)
                .doOnNext(created -> log.info("Created template with ID {}", created.getId()));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<NotificationTemplate>> update(@PathVariable String id,
            @RequestBody NotificationTemplate updated) {
        log.info("PUT /api/templates/{} - updating template", id);
        return templateRepository.findById(id)
                .flatMap(existing -> {
                    updated.setId(existing.getId());
                    return templateRepository.save(updated);
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Updated template with ID {}", id);
                    } else {
                        log.warn("Template with ID {} not found for update", id);
                    }
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        log.info("DELETE /api/templates/{} - deleting template", id);
        return templateRepository.existsById(id)
                .flatMap(exists -> {
                    if (exists) {
                        return templateRepository.deleteById(id)
                                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
                    } else {
                        log.warn("Template with ID {} not found for deletion", id);
                        return Mono.just(ResponseEntity.notFound().<Void>build());
                    }
                })
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Deleted template with ID {}", id);
                    }
                });
    }
}
