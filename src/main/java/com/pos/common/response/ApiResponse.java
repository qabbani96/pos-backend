package com.pos.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response wrapper for all endpoints.
 *
 * Success:  { success: true,  message: "...", data: {...} }
 * Error:    { success: false, message: "...", errorCode: "..." }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, message, null, errorCode);
    }
}
