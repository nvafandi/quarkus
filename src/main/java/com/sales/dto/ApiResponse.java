package com.sales.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Unified API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Whether the request was successful")
    private boolean success;

    @Schema(description = "HTTP status code")
    private int status;

    @Schema(description = "Response data payload")
    private T data;

    @Schema(description = "Error message when success is false")
    private String message;

    public ApiResponse() {}

    public ApiResponse(boolean success, int status, T data, String message) {
        this.success = success;
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, 201, data, "Resource created");
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(false, status, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
