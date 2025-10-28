package com.notification.controller;

import com.notification.entity.NotificationTemplate;
import com.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateController {

    private final NotificationTemplateRepository templateRepository;

    @GetMapping("/status")
    public String status() {
        log.info("GET /api/templates/status - checking service status");
        return "Notification service is up and running";
    }

    @GetMapping
    public List<NotificationTemplate> getAll() {
        log.info("GET /api/templates - fetching all templates");
        List<NotificationTemplate> templates = templateRepository.findAll();
        log.info("Found {} templates", templates.size());
        return templates;
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplate> getById(@PathVariable String id) {
        log.info("GET /api/templates/{} - fetching template by ID", id);
        Optional<NotificationTemplate> template = templateRepository.findById(id);
        template.ifPresent(value -> log.info("Found template with ID {}", id));
        return template.map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Template with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public NotificationTemplate create(@RequestBody NotificationTemplate template) {
        log.info("POST /api/templates - creating new template");
        NotificationTemplate createdTemplate = templateRepository.save(template);
        log.info("Created template with ID {}", createdTemplate.getId());
        return createdTemplate;
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplate> update(@PathVariable String id, @RequestBody NotificationTemplate updated) {
        log.info("PUT /api/templates/{} - updating template", id);
        return templateRepository.findById(id)
                .map(existing -> {
                    updated.setId(existing.getId());
                    NotificationTemplate savedTemplate = templateRepository.save(updated);
                    log.info("Updated template with ID {}", savedTemplate.getId());
                    return ResponseEntity.ok(savedTemplate);
                })
                .orElseGet(() -> {
                    log.warn("Template with ID {} not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/templates/{} - deleting template", id);
        if (templateRepository.existsById(id)) {
            templateRepository.deleteById(id);
            log.info("Deleted template with ID {}", id);
            return ResponseEntity.noContent().build();
        }
        log.warn("Template with ID {} not found for deletion", id);
        return ResponseEntity.notFound().build();
    }
}
