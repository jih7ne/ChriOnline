package com.chrionline.chrionline.network.protocol;

import com.chrionline.chrionline.network.enums.RequestType;
import com.chrionline.chrionline.core.exceptions.RequestException;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AppRequest {
    private String id;
    private String controller;
    private String action;
    private String payload;


    private Map<String, Object> parameters;
    private Map<String, String> headers;
    private long timestamp;

    private String clientId;
    private String clientVersion;
    private String authToken;

    private AppRequest() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.parameters = new HashMap<>();
        this.headers = new HashMap<>();
    }


    public String getId() { return id; }
    public String getController() { return controller; }
    public String getAction() { return action; }
    public String getPayload() { return payload; }
    public Map<String, Object> getParameters() { return parameters; }
    public Map<String, String> getHeaders() { return headers; }
    public long getTimestamp() { return timestamp; }
    public String getClientId() { return clientId; }
    public String getClientVersion() { return clientVersion; }
    public String getAuthToken() { return authToken; }


    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name, Class<T> type) {
        Object value = parameters.get(name);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }


        if (value instanceof String) {
            return JsonUtils.fromJson((String) value, type);
        }


        return JsonUtils.fromJson(JsonUtils.toJson(value), type);
    }

    public String getString(String name) {
        return getParameter(name, String.class);
    }

    public Integer getInt(String name) {
        return getParameter(name, Integer.class);
    }

    public Boolean getBoolean(String name) {
        return getParameter(name, Boolean.class);
    }

    public Double getDouble(String name) {
        return getParameter(name, Double.class);
    }

    public boolean hasParameter(String name) {
        return parameters.containsKey(name);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public boolean hasHeader(String name) {
        return headers.containsKey(name);
    }

    public void validateRequired(String... paramNames) {
        for (String paramName : paramNames) {
            if (!hasParameter(paramName)) {
                throw new RequestException("Missing required parameter: " + paramName);
            }
        }
    }

    public void validatePayload() {
        if (payload == null || payload.trim().isEmpty()) {
            throw new RequestException("Payload is required");
        }
    }

    public String toJson() {
        return JsonUtils.toJson(this);
    }


    public static AppRequest fromJson(String json) {
        try {
            return JsonUtils.fromJson(json, AppRequest.class);
        } catch (JsonSyntaxException e) {
            throw new RequestException("Invalid request JSON format", e);
        }
    }

    public <T> T getPayloadAs(Type type) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return JsonUtils.fromJson(payload, type);
        } catch (JsonSyntaxException e) {
            throw new RequestException("Invalid payload format for type: " + type.getTypeName(), e);
        }
    }

    public <T> java.util.List<T> getPayloadAsList(Class<T> elementType) {
        Type listType = TypeToken.getParameterized(java.util.List.class, elementType).getType();
        return getPayloadAs(listType);
    }



    public static class Builder {
        private final AppRequest appRequest;

        public Builder() {
            this.appRequest = new AppRequest();

        }

        public Builder controller(String controller) {
            appRequest.controller = controller;
            return this;
        }

        public Builder action(String action) {
            appRequest.action = action;
            return this;
        }

        public Builder payload(String payload) {
            appRequest.payload = payload;
            return this;
        }

        public Builder payload(Object payloadObject) {
            appRequest.payload = JsonUtils.toJson(payloadObject);
            return this;
        }



        public Builder parameter(String name, Object value) {
            appRequest.parameters.put(name, value);
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            appRequest.parameters.putAll(parameters);
            return this;
        }

        public Builder header(String name, String value) {
            appRequest.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            appRequest.headers.putAll(headers);
            return this;
        }

        public Builder clientId(String clientId) {
            appRequest.clientId = clientId;
            return this;
        }

        public Builder clientVersion(String version) {
            appRequest.clientVersion = version;
            return this;
        }

        public Builder authToken(String token) {
            appRequest.authToken = token;
            return this;
        }

        public Builder id(String id) {
            appRequest.id = id;
            return this;
        }

        /**
         * Build the request
         */
        public AppRequest build() {
            // Validate required fields
            if (appRequest.controller == null) {
                throw new RequestException("Controller is required");
            }
            if (appRequest.action == null) {
                throw new RequestException("Action is required");
            }
            return appRequest;
        }


        public String buildJson() {
            return build().toJson();
        }
    }


    @Override
    public String toString() {
        return String.format("Request[id=%s, controller=%s, action=%s, client=%s]",
                id, controller, action, clientId);
    }
}




