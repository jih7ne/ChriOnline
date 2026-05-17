package com.chrionline.network.utils;

public class NetworkUtils {


    public static String extractField(String json, String field) {
        try {
            int idx = json.indexOf("\"" + field + "\"");
            if (idx == -1) return "?";
            int colon = json.indexOf(":", idx);
            int start = json.indexOf("\"", colon) + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "?";
        }
    }
}
