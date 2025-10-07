package com.notification.service;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class TemplateEngineService {

    public String renderTemplate(String template, Map<String, Object> values) {
        String result = template;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
