package com.zkry.trip.dto.ai;

public record PythonAiCallResult<T>(
    boolean success,
    String message,
    T data,
    String rawJson
) {
    public static <T> PythonAiCallResult<T> ok(String message, T data, String rawJson) {
        return new PythonAiCallResult<>(true, message, data, rawJson);
    }

    public static <T> PythonAiCallResult<T> failure(String message) {
        return new PythonAiCallResult<>(false, message, null, null);
    }
}
