package com.chrionline.chrionline.server;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.network.protocol.AppNotification;
import com.chrionline.chrionline.network.udp.UDPServer;

import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerUDPTest {

    private static UDPServer udpServer;
    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);


    private static void startUdp() throws SocketException {

        // Set handler for incoming notifications
        udpServer.setNotificationHandler(notification -> {
            AppConfig.getLogger().info("Notification received: {}", notification.getMessage());
            // Optionally broadcast to other clients
            udpServer.broadcastNotification(notification);
        });

        udpServer.start();

        // Simulate sending periodic notifications
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            AppNotification notification = AppNotification.info(
                    "sys-" + System.currentTimeMillis(),
                    "Server heartbeat",
                    "notification-server"
            );

            // Broadcast to all clients
            udpServer.broadcastNotification(notification);

        }, 0, 5, TimeUnit.SECONDS);
    }


    private static void stopInstances(){
        if (udpServer != null) {
            udpServer.stop();
        }
        scheduledExecutorService.shutdown();
    }
}
