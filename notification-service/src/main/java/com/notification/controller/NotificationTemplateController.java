package com.notification.controller;

import com.notification.entity.NotificationTemplate;
import com.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateRepository templateRepository;

    @GetMapping("status")
    public String status(){
        return "Notification service is up and running";
    }

    // Get all templates
    @GetMapping
    public List<NotificationTemplate> getAll() {
        return templateRepository.findAll();
    }

    // Get template by ID
    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplate> getById(@PathVariable String id) {
        Optional<NotificationTemplate> template = templateRepository.findById(id);
        return template.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get template by eventType + channel
//    @GetMapping("/by-event")
//    public ResponseEntity<NotificationTemplate> getByEventTypeAndChannel(
//            @RequestParam String eventType,
//            @RequestParam String channel
//    ) {
//        Optional<NotificationTemplate> template = templateRepository.findByEventTypeAndChannelAndIsActiveTrue(eventType, channel);
//        return template.map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }

    // Create a new template
    @PostMapping
    public NotificationTemplate create(@RequestBody NotificationTemplate template) {
        return templateRepository.save(template);
    }

    // Update a template
    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplate> update(@PathVariable String id, @RequestBody NotificationTemplate updated) {
        return templateRepository.findById(id)
                .map(existing -> {
                    updated.setId(existing.getId());
                    return ResponseEntity.ok(templateRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete a template
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (templateRepository.existsById(id)) {
            templateRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
