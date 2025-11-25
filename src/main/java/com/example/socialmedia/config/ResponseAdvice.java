package com.example.socialmedia.config;

import com.example.socialmedia.payload.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Do not wrap if already ApiResponse or if dealing with Swagger/OpenAPI
        String className = returnType.getContainingClass().getName();
        return !className.contains("org.springdoc") && !className.contains("springfox");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // If body is null, treat as success with no data
        if (body == null) {
            return ApiResponse.success(null, "Success");
        }

        // If already wrapped, return as is
        if (body instanceof ApiResponse) {
            return body;
        }
        
        // If body is String, manually serialize because Spring treats String differently
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(ApiResponse.success(body, "Success"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return ApiResponse.success(body, "Success");
    }
}
