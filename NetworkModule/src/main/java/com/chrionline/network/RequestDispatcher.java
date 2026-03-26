package com.chrionline.network;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.interfaces.IController;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.protocol.RequestParser;

import java.lang.reflect.Method;

public class RequestDispatcher {

    public static String dispatch(String message) {
        try {
            String[] parts = message.split("#", 2);

            if (parts.length < 2) {
                return AppResponse.badRequest("ERROR: Invalid message format. Expected: Controller#action:payload");
            }

            String controllerName = parts[0];
            String[] actionPayload = parts[1].split(":", 2);

            String action = actionPayload[0];
            String payload = actionPayload.length > 1 ? actionPayload[1] : "";

            IController controller = ServerConfig.getController(controllerName);

            if(controller == null) {
                return AppResponse.error("Controller '" + controllerName + "' not found");
            }

            Method method = controller.getClass()
                    .getMethod(action.toLowerCase(), String.class);

            return (String) method.invoke(controller, payload);

        } catch (NoSuchMethodException e) {
            return AppResponse.error("Action not found: " + e.getMessage());
        } catch (Exception e) {
            return AppResponse.error(e.getMessage());
        }
    }


    public static String dispatch(AppRequest request) {
        try {

            RequestParser.validate(request);


            IController controller = ServerConfig.getController(request.getController());
            if (controller == null) {
                return AppResponse.error("Controller '" + request.getController() + "' not found");
            }


            Method method = findActionMethod(controller, request.getAction());


            return (String) method.invoke(controller, request);

        } catch (NoSuchMethodException e) {
            return AppResponse.error("Action '" + request.getAction() + "' not found in controller '" +
                    request.getController() + "'");
        } catch (Exception e) {
            return AppResponse.error(e.getMessage());
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
