package com.cexwallet.api.common;

public record ApiError(String code, String message, Object details) {
}

