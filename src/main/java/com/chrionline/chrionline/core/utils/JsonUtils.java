package com.chrionline.chrionline.core.utils;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;


public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static final Gson networkGson = new GsonBuilder()
            .serializeNulls()
            .create();

    //Serialization

    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        return networkGson.toJson(object);
    }


    public static String toPrettyJson(Object object) {
        if (object == null) {
            return "null";
        }
        return gson.toJson(object);
    }


    //Deserialization
    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            return networkGson.fromJson(json, classOfT);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse JSON: {}", json, e);
            throw new RuntimeException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            return networkGson.fromJson(json, typeOfT);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse JSON: {}", json, e);
            throw new RuntimeException("Invalid JSON format: " + e.getMessage(), e);
        }
    }


    public static <T> T getDataAs(Class<T> type, Object data) {
        if (data == null) {
            return null;
        }

        if (type.isInstance(data)) {
            return type.cast(data);
        }

        return JsonUtils.fromJson(JsonUtils.toJson(data), type);
    }


}
