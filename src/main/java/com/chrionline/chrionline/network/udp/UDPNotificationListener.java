package com.chrionline.chrionline.network.udp;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.protocol.AppNotification;


import java.net.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class UDPNotificationListener {
    private DatagramSocket clientSocket;
    private volatile boolean listening = false;
    private Thread listenerThread;
    private byte[] receiveBuffer;
    private int timeout = AppConstants.SOCKET_TIMEOUT_MS;
    private int currentBufferSize;


    // Optional: Queue for collecting notifications
    private final BlockingQueue<AppNotification> notificationQueue = new LinkedBlockingQueue<>();

    // Optional: Callback for real-time processing
    private Consumer<AppNotification> notificationCallback;

    public UDPNotificationListener() throws SocketException, UnknownHostException {
        initializeSocket();
    }


    public void startListening() {
        if (listening) {
            AppConfig.getLogger().warn("Listener already running");
            return;
        }

        this.listening = true;

        listenerThread = new Thread(() -> {
            AppConfig.getLogger().info("Started notification listener thread");

            while (listening && !Thread.currentThread().isInterrupted()) {
                try {
                    AppNotification notification = receiveNotification();

                    // Add to queue for later retrieval
                    notificationQueue.offer(notification);

                    // Trigger callback if set
                    if (notificationCallback != null) {
                        notificationCallback.accept(notification);
                    }

                    AppConfig.getLogger().debug("Received notification: {}", notification.getId());

                } catch (SocketTimeoutException e) {
                    // Timeout is normal - just continue listening
                    continue;
                } catch (IOException e) {
                    if (listening) {
                        AppConfig.getLogger().error("Error receiving notification: {}", e.getMessage());
                        handleReconnection();
                    }
                }
            }
        }, "UDP-Notification-Listener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }


    private void handleReconnection() {
        try {
            Thread.sleep(AppConstants.RECONNECT_DELAY_MS);
            if (listening) {
                close();
                initializeSocket();
                AppConfig.getLogger().info("Reconnected to server");
            }
        } catch (Exception e) {
            AppConfig.getLogger().error("Reconnection failed: {}", e.getMessage());
        }
    }


    public void stopListening() {
        listening = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                listenerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        AppConfig.getLogger().info("Stopped continuous notification listener");
    }


    private void initializeSocket() throws SocketException, UnknownHostException {
        this.currentBufferSize = AppConstants.BUFFER_SIZE;
        this.clientSocket = new DatagramSocket();
        this.clientSocket.setSoTimeout(AppConstants.SOCKET_TIMEOUT_MS);
        this.receiveBuffer = new byte[currentBufferSize];

        AppConfig.getLogger().info("UDP Notification Listener initialized on random port");
        AppConfig.getLogger().info("Listening for notifications from server {}:{}",
                AppConstants.SERVER_HOST, AppConstants.SERVER_PORT);
    }



    public AppNotification receiveNotification() throws IOException {
        return AppNotification.fromJson(receiveRawNotification());
    }


    private String receiveRawNotification() throws IOException {
        DatagramPacket receivePacket = new DatagramPacket(
                receiveBuffer,
                receiveBuffer.length
        );

        try {
            clientSocket.receive(receivePacket);

            if (receivePacket.getLength() == receiveBuffer.length) {
                AppConfig.getLogger().warn("Buffer may have been too small. Consider increasing size.");
            }

            return new String(receivePacket.getData(), 0, receivePacket.getLength(),
                    StandardCharsets.UTF_8);

        } catch (SocketTimeoutException e) {
            AppConfig.getLogger().debug("Socket timeout - no notification received");
            throw e;
        } catch (SocketException e) {
            if (e.getMessage() != null && e.getMessage().contains("buffer")) {
                return handleBufferOverflow();
            }
            throw e;
        }
    }



    public boolean isConnected() {
        return clientSocket != null && !clientSocket.isClosed() && listening;
    }

    public void close() {
        stopListening();
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
            AppConfig.getLogger().info("UDP Notification Listener closed");
        }
    }


    public void setTimeout(int timeout) {
        this.timeout = timeout;
        try {
            clientSocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            AppConfig.getLogger().error("Failed to set timeout: {}", e.getMessage());
        }
    }




    private String handleBufferOverflow() throws IOException {
        if (currentBufferSize >= AppConstants.MAX_BUFFER_SIZE) {
            throw new IOException("Buffer already at maximum size: " + currentBufferSize);
        }

        currentBufferSize = Math.min(currentBufferSize * 2, AppConstants.MAX_BUFFER_SIZE);
        receiveBuffer = new byte[currentBufferSize];
        AppConfig.getLogger().info("Increased buffer size to: {}", currentBufferSize);

        // Retry receiving
        return receiveRawNotification();
    }


    public AppNotification pollNotification() {
        return notificationQueue.poll();
    }

    public AppNotification takeNotification() throws InterruptedException {
        return notificationQueue.take();
    }

    public int getPendingNotificationCount() {
        return notificationQueue.size();
    }


    public void clearPendingNotifications() {
        notificationQueue.clear();
        AppConfig.getLogger().debug("Cleared pending notifications");
    }


    public void setNotificationCallback(Consumer<AppNotification> callback) {
        this.notificationCallback = callback;
    }


}