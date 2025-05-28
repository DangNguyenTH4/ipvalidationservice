package com.sunteco.ipvalidation.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JacksonUtils {
    private static ObjectMapper objectMapper;
    private static ObjectMapper objectMapperWrite;
    static {
        objectMapper = new ObjectMapper();
        objectMapperWrite = new ObjectMapper();
        objectMapperWrite.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapperWrite.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    public static String write(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to write object to json");
            return null;
        }
    }
    public static String writeWithValue(Object object) {
        try {
            return objectMapperWrite.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to write object to json");
            return null;
        }
    }
    public static <T> T readValue(String json, Class<T> valueType) {
//        log.info("Read json to object json: {} classType: {},", StringUtils.substring(json,0,30), valueType);
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(String json, TypeReference<T> t) {
        try {
            return objectMapper.readValue(json, t);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readValue(byte[] data, Class<T> valueType) {
//        log.info("Read json to object json: {} classType: {},", StringUtils.substring(json,0,30), valueType);
        try {
            return objectMapper.readValue(data, valueType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T convert(Object object, Class<T> valueType) {
        return objectMapper.convertValue(object, valueType);
    }

    public static <T> T convert(Object object, TypeReference<T> t) {
        return objectMapper.convertValue(object, t);
    }

    public static <T> List<T> convertList(Object object, TypeReference<List<T>> t) {
        try {
            return objectMapper.convertValue(object, t);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> List<T> convertList(String object, TypeReference<List<T>> t) {
        try {
            return objectMapper.readValue(object, t);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


}
