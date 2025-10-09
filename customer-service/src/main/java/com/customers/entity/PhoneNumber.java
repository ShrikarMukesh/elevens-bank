package com.customers.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumber {
    private String type;
    private String number;
    private boolean isPrimary;  // ✅ Added
}
