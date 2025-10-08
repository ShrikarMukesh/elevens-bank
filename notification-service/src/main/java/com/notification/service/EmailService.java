package com.notification.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        to = "mukesh.shrikar11@gmail.com";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to); // In real case, lookup user email by customerId
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("📧 Email sent to {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send email: {}", e.getMessage());
        }
    }
}
