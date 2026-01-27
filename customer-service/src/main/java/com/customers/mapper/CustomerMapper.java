package com.customers.mapper;

import com.customers.entity.*;
import com.customers.model.*;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomerMapper {

    public CustomerDto toDto(Customer entity) {
        if (entity == null) {
            return null;
        }
        CustomerDto dto = new CustomerDto();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setUserId(entity.getUserId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        if (entity.getDob() != null) {
            dto.setDob(entity.getDob());
        }
        dto.setGender(entity.getGender());
        dto.setEmail(entity.getEmail());
        dto.setStatus(entity.getStatus());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }

        dto.setPhoneNumbers(toPhoneNumberDtos(entity.getPhoneNumbers()));
        dto.setAddresses(toAddressDtos(entity.getAddresses()));
        dto.setKyc(toKycDto(entity.getKyc()));
        dto.setPreferences(toPreferencesDto(entity.getPreferences()));

        return dto;
    }

    public Customer toEntity(CustomerDto dto) {
        if (dto == null) {
            return null;
        }
        Customer entity = new Customer();
        entity.setId(dto.getId());
        entity.setCustomerId(dto.getCustomerId());
        entity.setUserId(dto.getUserId());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setDob(dto.getDob()); // LocalDate maps directly usually
        entity.setGender(dto.getGender());
        entity.setEmail(dto.getEmail());
        entity.setStatus(dto.getStatus());
        // createdAt/updatedAt usually managed by DB/Auditing, but can map if needed
        if (dto.getCreatedAt() != null) {
            entity.setCreatedAt(dto.getCreatedAt().toInstant());
        }

        entity.setPhoneNumbers(toPhoneNumberEntities(dto.getPhoneNumbers()));
        entity.setAddresses(toAddressEntities(dto.getAddresses()));
        entity.setKyc(toKycEntity(dto.getKyc()));
        entity.setPreferences(toPreferencesEntity(dto.getPreferences()));

        return entity;
    }

    // Helpers for nested objects

    private List<PhoneNumberDto> toPhoneNumberDtos(List<PhoneNumber> entities) {
        if (entities == null)
            return null;
        return entities.stream().map(e -> {
            PhoneNumberDto d = new PhoneNumberDto();
            d.setType(e.getType());
            d.setNumber(e.getNumber());
            d.setIsPrimary(e.isPrimary());
            return d;
        }).collect(Collectors.toList());
    }

    private List<PhoneNumber> toPhoneNumberEntities(List<PhoneNumberDto> dtos) {
        if (dtos == null)
            return null;
        return dtos.stream().map(d -> {
            PhoneNumber e = new PhoneNumber();
            e.setType(d.getType());
            e.setNumber(d.getNumber());
            e.setPrimary(Boolean.TRUE.equals(d.getIsPrimary()));
            return e;
        }).collect(Collectors.toList());
    }

    private List<AddressDto> toAddressDtos(List<Address> entities) {
        if (entities == null)
            return null;
        return entities.stream().map(e -> {
            AddressDto d = new AddressDto();
            d.setType(e.getType());
            d.setLine1(e.getLine1());
            d.setCity(e.getCity());
            d.setState(e.getState());
            d.setPincode(e.getPincode());
            d.setCountry(e.getCountry());
            d.setIsPrimary(e.isPrimary());
            return d;
        }).collect(Collectors.toList());
    }

    private List<Address> toAddressEntities(List<AddressDto> dtos) {
        if (dtos == null)
            return null;
        return dtos.stream().map(d -> {
            Address e = new Address();
            e.setType(d.getType());
            e.setLine1(d.getLine1());
            e.setCity(d.getCity());
            e.setState(d.getState());
            e.setPincode(d.getPincode());
            e.setCountry(d.getCountry());
            e.setPrimary(Boolean.TRUE.equals(d.getIsPrimary()));
            return e;
        }).collect(Collectors.toList());
    }

    private KycDetailsDto toKycDto(KycDetails entity) {
        if (entity == null)
            return null;
        KycDetailsDto dto = new KycDetailsDto();
        // PII fields are now handled by kyc-service. We do not map them here.
        // dto.setAadhaar(entity.getAadhaar());
        // dto.setPan(entity.getPan());
        // dto.setPassport(entity.getPassport());
        dto.setVerified("VERIFIED".equals(entity.getStatus()));
        if (entity.getVerifiedAt() != null) {
            dto.setVerifiedAt(entity.getVerifiedAt().atOffset(ZoneOffset.UTC));
        }
        return dto;
    }

    private KycDetails toKycEntity(KycDetailsDto dto) {
        if (dto == null)
            return null;
        KycDetails entity = new KycDetails();
        // PII fields are not stored in KycDetails anymore.
        // entity.setAadhaar(dto.getAadhaar());
        // entity.setPan(dto.getPan());
        // entity.setPassport(dto.getPassport());
        entity.setStatus(Boolean.TRUE.equals(dto.getVerified()) ? "VERIFIED" : "PENDING");
        if (dto.getVerifiedAt() != null) {
            entity.setVerifiedAt(dto.getVerifiedAt().toInstant());
        }
        return entity;
    }

    private PreferencesDto toPreferencesDto(Preferences entity) {
        if (entity == null)
            return null;
        PreferencesDto dto = new PreferencesDto();
        dto.setLanguage(entity.getLanguage());
        dto.setNotifications(entity.isNotifications());
        dto.setChannels(entity.getChannels());
        return dto;
    }

    private Preferences toPreferencesEntity(PreferencesDto dto) {
        if (dto == null)
            return null;
        Preferences entity = new Preferences();
        entity.setLanguage(dto.getLanguage());
        entity.setNotifications(Boolean.TRUE.equals(dto.getNotifications()));
        entity.setChannels(dto.getChannels());
        return entity;
    }
}
