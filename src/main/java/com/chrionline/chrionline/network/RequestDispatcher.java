package com.chrionline.chrionline.network;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.interfaces.IController;

import java.lang.reflect.Method;

public class RequestDispatcher {

    public static String dispatch(String message) {
        try {
            String[] parts = message.split("#", 2); // Limit to 2 parts

            if (parts.length < 2) {
                return "ERROR: Invalid message format. Expected: Controller#action:payload";
            }

            String controllerName = parts[0];
            String[] actionPayload = parts[1].split(":", 2);

            String action = actionPayload[0];
            String payload = actionPayload.length > 1 ? actionPayload[1] : "";

            IController controller = AppConfig.getController(controllerName);

            if(controller == null) {
                return "ERROR: Controller '" + controllerName + "' not found";
            }

            Method method = controller.getClass()
                    .getMethod(action.toLowerCase(), String.class);

            return (String) method.invoke(controller, payload);

        } catch (NoSuchMethodException e) {
            return "ERROR: Action not found: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
