package com.chrionline.chrionline.client;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.network.udp.UDPNotificationListener;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientUDPTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final UDPNotificationListener listener;

    public ClientUDPTest() throws SocketException, UnknownHostException {
        listener = new UDPNotificationListener();

        listener.setNotificationHandler(notification -> {
            //dbService.save(notification);
            //cacheService.invalidate(notification.getId());
            //auditLog.record(notification);
            System.out.println(notification.toJson());
        }, executor);

        listener.startListening();
    }

    public void stop() {
        listener.close();
        executor.shutdown();
    }

    public static void main(String[] args) throws Exception {
        ClientUDPTest client = new ClientUDPTest();

        AppConfig.getLogger().info("Client listening for notifications. Press Ctrl+C to stop.");

        // Shutdown hook — fires on Ctrl+C or SIGTERM so we clean up gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AppConfig.getLogger().info("Shutdown signal received — stopping client...");
            client.stop();
            AppConfig.getLogger().info("Client stopped. Total notifications received: {}",
                    client.listener.getReceivedCount());
        }));

        // Keep the main thread alive — the listener runs on its own daemon thread
        // so without this the JVM would exit immediately after main() returns.
        Thread.currentThread().join();
    }
}
