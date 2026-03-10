package com.chrionline.chrionline.network.protocol;

import com.chrionline.chrionline.core.utils.JsonUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiResponse {
    private String status;
    private String message;
    private Object data;
    private Map<String, Object> metadata;


    private ApiResponse(){
        this.metadata = new ConcurrentHashMap<>();
    }


    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    //Serializing ServerResponse

    public String toJson() {
        return JsonUtils.toJson(this);
    }

    //Deserializing to ServerResponse
    public static ApiResponse fromJson(String json) {
        return JsonUtils.fromJson(json, ApiResponse.class);
    }

    public <T> T getDataAs(Class<T> type) {
        if (data == null) {
            return null;
        }
        // If data is already the right type, return it
        if (type.isInstance(data)) {
            return type.cast(data);
        }
        // Otherwise, try to convert from JSON
        return JsonUtils.fromJson(JsonUtils.toJson(data), type);
    }


    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }



    public static class Builder {
        private final ApiResponse response;

        public Builder() {
            this.response = new ApiResponse();
        }

        public Builder success() {
            response.status = "SUCCESS";
            return this;
        }

        public Builder error() {
            response.status = "ERROR";
            return this;
        }

        public Builder status(String status) {
            response.status = status;
            return this;
        }

        public Builder message(String message) {
            response.message = message;
            return this;
        }

        public Builder data(Object data) {
            response.data = data;
            return this;
        }

        public Builder metadata(String key, Object value) {
            response.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            response.metadata.putAll(metadata);
            return this;
        }

        public ApiResponse build() {
            return response;
        }

        public String buildJson() {
            return response.toJson();
        }
    }


    /**
     * Create a success response with data
     */

    public static String success(Object data) {
        return new Builder()
                .success()
                .data(data)
                .buildJson();
    }

    /**
     * Create a success response with data and message
     */
    public static String success(Object data, String message) {
        return new Builder()
                .success()
                .data(data)
                .message(message)
                .buildJson();
    }

    /**
     * Create a success response with metadata
     */
    public static String success(Object data, Map<String, Object> metadata) {
        return new Builder()
                .success()
                .data(data)
                .metadata(metadata)
                .buildJson();
    }

    /**
     * Create an error response with message
     */
    public static String error(String message) {
        return new Builder()
                .error()
                .message(message)
                .buildJson();
    }

    /**
     * Create an error response with data and message
     */
    public static String error(String message, Object errorData) {
        return new Builder()
                .error()
                .message(message)
                .data(errorData)
                .buildJson();
    }


    public static String withData(Object data) {
        return ApiResponse.success(data);
    }


    public static String withData(Object data, String message) {
        return ApiResponse.success(data, message);
    }

    public static String withData(Object data, Map<String, Object> metadata) {
        return ApiResponse.success(data, metadata);
    }

    public static String ok() {
        return ApiResponse.success(null, "Operation completed successfully");
    }

    public static String notFound(String resource) {
        return ApiResponse.error(resource + " not found");
    }


    public static String badRequest(String message) {
        return ApiResponse.error("Bad request: " + message);
    }

    public static String unauthorized(String message) {
        return ApiResponse.error("Unauthorized: " + message);
    }

    /**
     * Create a simple string response (for backward compatibility)
     */
    public static String simple(String text) {
        return new Builder()
                .status("MESSAGE")
                .data(text)
                .buildJson();
    }

    /**
     * Create a null response
     */
    public static String noContent() {
        return new Builder()
                .success()
                .message("No content")
                .buildJson();
    }


}
