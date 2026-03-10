package com.chrionline.chrionline.network.protocol;


import com.chrionline.chrionline.network.enums.RequestType;
import com.chrionline.chrionline.core.exceptions.RequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class RequestParser {
    private static final Logger logger = LoggerFactory.getLogger(RequestParser.class);


    public static AppRequest parse(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new RequestException("Empty request message");
        }

        message = message.trim();

        // Try parsing as JSON first
        if (message.startsWith("{")) {
            try {
                return parseJson(message);
            } catch (Exception e) {
                logger.warn("Failed to parse as JSON, trying simple format: {}", e.getMessage());
            }
        }

        // Fall back to simple format: controller#action:payload
        return parseSimple(message);
    }

    /**
     * Parse JSON format request
     * Example: {"controller":"produit","action":"get","payload":"123","headers":{"auth":"token"}}
     */
    private static AppRequest parseJson(String json) {
        return AppRequest.fromJson(json);
    }

    /**
     * Parse simple format request
     * Example: produit#get:123
     * Or: produit#get: with parameters
     */
    private static AppRequest parseSimple(String message) {
        String[] parts = message.split("#", 2);

        if (parts.length < 2) {
            throw new RequestException("Invalid simple format. Expected: controller#action[:payload]");
        }

        String controller = parts[0];
        String remaining = parts[1];

        String[] actionParts = remaining.split(":", 2);
        String action = actionParts[0];
        String payload = actionParts.length > 1 ? actionParts[1] : "";

        return new AppRequest.Builder()
                .controller(controller)
                .action(action)
                .payload(payload)
                .build();
    }

    /**
     * Parse with additional context (client info)
     */
    public static AppRequest parseWithContext(String message, String clientId, String clientAddress) {
        AppRequest request = parse(message);

        // Add client metadata
        return new AppRequest.Builder()
                .controller(request.getController())
                .action(request.getAction())
                .payload(request.getPayload())
                .parameters(request.getParameters())
                .headers(request.getHeaders())
                .clientId(clientId)
                .header("client-address", clientAddress)
                .build();
    }

    /**
     * Validate request has required fields
     */
    public static void validate(AppRequest request) {
        if (request.getController() == null) {
            throw new RequestException("Controller is required");
        }
        if (request.getAction() == null) {
            throw new RequestException("Action is required");
        }
    }



}
