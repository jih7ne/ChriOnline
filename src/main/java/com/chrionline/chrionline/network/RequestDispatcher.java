package com.chrionline.chrionline.network;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.network.protocol.ApiResponse;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.RequestParser;

import java.lang.reflect.Method;

public class RequestDispatcher {

    public static String dispatch(String message) {
        try {
            String[] parts = message.split("#", 2);

            if (parts.length < 2) {
                return ApiResponse.badRequest("ERROR: Invalid message format. Expected: Controller#action:payload");
            }

            String controllerName = parts[0];
            String[] actionPayload = parts[1].split(":", 2);

            String action = actionPayload[0];
            String payload = actionPayload.length > 1 ? actionPayload[1] : "";

            IController controller = AppConfig.getController(controllerName);

            if(controller == null) {
                return ApiResponse.error("Controller '" + controllerName + "' not found");
            }

            Method method = controller.getClass()
                    .getMethod(action.toLowerCase(), String.class);

            return (String) method.invoke(controller, payload);

        } catch (NoSuchMethodException e) {
            return ApiResponse.error("Action not found: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }


    public static String dispatch(AppRequest request) {
        try {

            RequestParser.validate(request);


            IController controller = AppConfig.getController(request.getController());
            if (controller == null) {
                return ApiResponse.error("Controller '" + request.getController() + "' not found");
            }


            Method method = findActionMethod(controller, request.getAction());


            return (String) method.invoke(controller, request);

        } catch (NoSuchMethodException e) {
            return ApiResponse.error("Action '" + request.getAction() + "' not found in controller '" +
                    request.getController() + "'");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }


    private static Method findActionMethod(IController controller, String action)
            throws NoSuchMethodException {


        try {
            return controller.getClass().getMethod(action, AppRequest.class);
        } catch (NoSuchMethodException e) {

             return controller.getClass().getMethod(action, String.class);
        }

    }
}
