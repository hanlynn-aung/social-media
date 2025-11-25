package com.example.socialmedia.util;

import com.example.socialmedia.payload.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }
    
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    public static ResponseEntity<ApiResponse<Void>> error(String message, HttpStatus status) {
        // Always return 200 OK, but with error body
        return ResponseEntity.ok(ApiResponse.error(message));
    }
    
    public static ResponseEntity<ApiResponse<Void>> notFound(String message) {
        return error(message, HttpStatus.NOT_FOUND);
    }
    
    public static ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return error(message, HttpStatus.BAD_REQUEST);
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(T data, String message) {
        return ResponseEntity.ok(ApiResponse.error(data, message));
    }

    /**
     * Build error response for use in controllers
     */
    public static Object buildErrorResponse(String message) {
        return ApiResponse.error(message);
    }

    /**
     * Build success response for use in controllers
     */
    public static Object buildSuccessResponse(String message) {
        return ApiResponse.success(null, message);
    }
}
