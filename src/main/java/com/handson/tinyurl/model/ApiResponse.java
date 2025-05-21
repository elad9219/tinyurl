package com.handson.tinyurl.model;

import java.util.List;

public class ApiResponse<T> {
    private List<T> data;
    private String message;

    public ApiResponse(List<T> data, String message) {
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(List<T> data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> empty(String message) {
        return new ApiResponse<>(null, message);
    }

    public List<T> getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}