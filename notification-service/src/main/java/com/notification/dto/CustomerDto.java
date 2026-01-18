package com.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDto {
    private String id;
    private String customerId;
    private String userId;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String gender;
    private String email;
    private List<PhoneNumberDto> phoneNumbers;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhoneNumberDto {
        private String type;
        private String number;
        
        @JsonProperty("isPrimary")
        private boolean isPrimary;
    }
}
