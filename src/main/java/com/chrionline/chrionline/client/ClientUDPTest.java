package com.chrionline.chrionline.client;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.protocol.AppNotification;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.network.udp.UDPNotificationListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientUDPTest {

    private static UDPNotificationListener notificationListener;
    private List<AppNotification> notifications = new ArrayList<>();


    public static void startNotificationListener() {
        try {
            // Create and start listener
            notificationListener = new UDPNotificationListener();

            // Optional: Set callback for real-time updates
            notificationListener.setNotificationCallback(notification -> {
                // Update UI or trigger events
                //updateNotificationPanel(notification);
            });

            // Start listening
            notificationListener.startListening();

            // Later, you can access notifications:
            // 1. From callback (real-time)
            // 2. From queue (batch processing)
            // 3. From your own collection (if you store them)

        } catch (Exception e) {
            AppConfig.getLogger().error(e.getMessage());
        }
    }


    public static void main(String[] args) {
        AppConfig.getLogger().info("Starting application...");
        AppConfig.getLogger().info("Starting UDP Notification Listener...");
        startNotificationListener();
    }
}
