package com.homer.core.configurations;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
@Slf4j
public class JSONArrayConverter implements AttributeConverter<JSONArray, String> {

    @Override
    public String convertToDatabaseColumn(JSONArray array)
    {
        String data = null;
        try
        {
            data = array.toString();
        }
        catch (final Exception e)
        {
            log.error("JSON writing error" + e);
        }

        return data;
    }

    @Override
    public JSONArray convertToEntityAttribute(String data)
    {
        JSONArray array = null;

        try
        {
            array = new JSONArray(data);
        }
        catch (final Exception e)
        {
            log.error("JSON reading error" + e);
        }

        return array;
    }
}