package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.protocol.AppRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestClientController implements IController {

    public String test(AppRequest request) {

        // Create a simple response object
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("message", "Test successful!");
        responseData.put("timestamp", System.currentTimeMillis());
        responseData.put("requestId", request.getId());
        responseData.put("randomValue", Math.random());
        responseData.put("uuid", UUID.randomUUID().toString());
        responseData.put("received", request.getPayload());

        // Return as success response with data
        return AppResponse.withData(responseData, "Test completed");
    }

}
