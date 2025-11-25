package com.example.socialmedia.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {

    private final ObjectMapper objectMapper;

    // Default instance
    private static final JsonUtil DEFAULT_INSTANCE = new JsonUtilBuilder().build();

    private JsonUtil(JsonUtilBuilder builder) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        if (builder.failOnUnknownProperties != null) {
            this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, builder.failOnUnknownProperties);
        }
        if (builder.failOnEmptyBeans != null) {
            this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, builder.failOnEmptyBeans);
        }
        if (builder.indentOutput != null && builder.indentOutput) {
            this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        if (builder.serializationInclusion != null) {
            this.objectMapper.setSerializationInclusion(builder.serializationInclusion);
        }
    }
    
    public static JsonUtilBuilder builder() {
        return new JsonUtilBuilder();
    }

    // Instance methods
    public String toJsonInstance(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }

    public <T> T fromJsonInstance(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to object", e);
            throw new RuntimeException("Error converting JSON to object", e);
        }
    }

    // Static wrappers
    public static String toJson(Object object) {
        return DEFAULT_INSTANCE.toJsonInstance(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return DEFAULT_INSTANCE.fromJsonInstance(json, clazz);
    }
    
    // Custom Builder
    public static class JsonUtilBuilder implements Builder<JsonUtil> {
        private Boolean failOnUnknownProperties;
        private Boolean failOnEmptyBeans;
        private Boolean indentOutput;
        private JsonInclude.Include serializationInclusion;

        public JsonUtilBuilder failOnUnknownProperties(boolean fail) {
            this.failOnUnknownProperties = fail;
            return this;
        }

        public JsonUtilBuilder failOnEmptyBeans(boolean fail) {
            this.failOnEmptyBeans = fail;
            return this;
        }
        
        public JsonUtilBuilder indentOutput(boolean indent) {
            this.indentOutput = indent;
            return this;
        }
        
        public JsonUtilBuilder serializationInclusion(JsonInclude.Include inclusion) {
            this.serializationInclusion = inclusion;
            return this;
        }

        @Override
        public JsonUtil build() {
            return new JsonUtil(this);
        }
    }
}

