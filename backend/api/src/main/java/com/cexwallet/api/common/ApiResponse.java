package com.cexwallet.api.common;

public record ApiResponse<T>(boolean success, T data, String message, ApiError error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "ok", null);
    }

    public static ApiResponse<Void> fail(String code, String message, Object details) {
        return new ApiResponse<>(false, null, null, new ApiError(code, message, details));
    }
}

