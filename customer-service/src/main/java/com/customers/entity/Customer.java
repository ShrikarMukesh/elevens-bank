package com.customers.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
@CompoundIndexes({
        @CompoundIndex(name = "idx_customerId", def = "{'customerId': 1}", unique = true),
        @CompoundIndex(name = "idx_email", def = "{'email': 1}"),
        @CompoundIndex(name = "idx_kyc_pan", def = "{'kyc.pan': 1}"),
        @CompoundIndex(name = "idx_kyc_aadhaar", def = "{'kyc.aadhaar': 1}"),
        @CompoundIndex(name = "idx_status", def = "{'status': 1}")
})
public class Customer {

    @Id
    private String id;  // MongoDB internal _id

    @Indexed(unique = true)
    private String customerId; // Generated internally in Customer Service

    @Field("userId")
    private String userId; // 🔗 reference to Auth Service user

    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String gender;

    @Indexed
    private String email;

    private List<PhoneNumber> phoneNumbers;
    private List<Address> addresses;
    private KycDetails kyc;
    private Preferences preferences;

    private String status; // ACTIVE, INACTIVE, SUSPENDED, CLOSED

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
