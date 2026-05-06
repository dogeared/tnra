package com.afitnerd.tnra.model.converter;

import com.afitnerd.tnra.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedIntegerConverter implements AttributeConverter<Integer, String> {

    @Lazy
    @Autowired
    EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(Integer attribute) {
        if (attribute == null) return null;
        return encryptionService.encrypt(String.valueOf(attribute));
    }

    @Override
    public Integer convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String decrypted = encryptionService.decrypt(dbData);
        return decrypted == null ? null : Integer.parseInt(decrypted);
    }
}
