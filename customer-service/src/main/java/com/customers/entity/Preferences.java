package com.customers.entity;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Preferences {
    private String language;
    private boolean notifications;
    private List<String> channels;
}
