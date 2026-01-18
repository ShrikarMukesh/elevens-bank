package com.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.from.number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    public void sendSms(String to, String body) {
        // Prepend +91 if the number is 10 digits and doesn't start with +
        if (to != null && to.matches("\\d{10}")) {
            to = "+91" + to;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromNumber),
                    body).create();
            log.info("📱 Hi Sachin SMS sent to {}. SID: {}", to, message.getSid());
        } catch (Exception e) {
            log.error("❌ Failed to send SMS: {}", e.getMessage());
        }
    }
}
