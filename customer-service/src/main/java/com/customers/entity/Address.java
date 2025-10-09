package com.customers.entity;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String type;
    private String line1;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private boolean isPrimary;  // ✅ Added
}
