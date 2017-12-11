package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.PostState;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class PostStateConverter implements AttributeConverter<PostState, String> {

    @Override
    public String convertToDatabaseColumn(PostState postState) {
        return postState.getValue();
    }

    @Override
    public PostState convertToEntityAttribute(String value) {
        return PostState.fromValue(value);
    }
}
