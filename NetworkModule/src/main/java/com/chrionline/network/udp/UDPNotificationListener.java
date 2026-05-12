package com.chrionline.network.udp;

import com.chrionline.core.constants.AppConstants;
import com.chrionline.network.protocol.AppNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class UDPNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(UDPNotificationListener.class);
    private DatagramSocket clientSocket;
    private volatile boolean listening = false;
    private Thread listenerThread;
    private byte[] receiveBuffer;
    private int currentBufferSize;

    private final List<AppNotification> receivedNotifications = new CopyOnWriteArrayList<>();

    private Consumer<AppNotification> notificationHandler;
    private ExecutorService handlerExecutor;

    public UDPNotificationListener() throws SocketException, UnknownHostException {
        initializeSocket();
    }

    public void setNotificationHandler(Consumer<AppNotification> handler, ExecutorService executor) {
        this.notificationHandler = handler;
        this.handlerExecutor     = executor;
    }

    public void startListening() {
        if (listening) { logger.warn("Listener already running"); return; }
        listening = true;

        listenerThread = new Thread(() -> {
            logger.info("Started notification listener thread");
            while (listening && !Thread.currentThread().isInterrupted()) {
                try {
                    AppNotification notification = receiveNotification();
                    if (notification != null) {
                        receivedNotifications.add(notification);
                        dispatchToHandler(notification);
                    }
                } catch (SocketTimeoutException e) {
                    // expected — just loop
                } catch (IOException e) {
                    if (listening) {
                        logger.error("Error receiving notification: {}", e.getMessage());
                        handleReconnection();
                    }
                }
            }
        }, "UDP-Notification-Listener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void initializeSocket() throws SocketException, UnknownHostException {
        this.currentBufferSize = AppConstants.BUFFER_SIZE;
        this.clientSocket      = new DatagramSocket();
        this.clientSocket.setSoTimeout(AppConstants.SOCKET_TIMEOUT_MS);
        this.receiveBuffer     = new byte[currentBufferSize];

        logger.info("UDP Notification Listener initialized");
        logger.info("Listening for notifications from server {}:{}",
                AppConstants.SERVER_HOST, AppConstants.UDP_PORT);

        registerWithServer(null);
    }

    /**
     * Register with the server.
     * @param userId null = anonymous (before login),
     *               integer = after login so server routes targeted notifications here only.
     */
    public void registerWithServer(Integer userId) {
        try {
            String message = (userId != null) ? "REGISTER:" + userId : "REGISTER";

            AppNotification notification = new AppNotification.Builder()
                    .message(message)
                    .build();
            notification.sign();

            byte[] data = notification.toJson().getBytes(StandardCharsets.UTF_8);
            DatagramPacket registration = new DatagramPacket(
                    data, data.length,
                    InetAddress.getByName(AppConstants.SERVER_HOST),
                    AppConstants.UDP_PORT
            );
            clientSocket.send(registration);

            if (userId != null) {
                logger.info("Re-registered with server as user {} on port {}",
                        userId, clientSocket.getLocalPort());
            } else {
                logger.info("Registered with server (anonymous) on port {}",
                        clientSocket.getLocalPort());
            }
        } catch (IOException e) {
            logger.error("Failed to register with server: {}", e.getMessage());
        }
    }

    private void handleReconnection() {
        try {
            Thread.sleep(AppConstants.RECONNECT_DELAY_MS);
            if (listening) {
                closeSocket();
                initializeSocket();
                logger.info("Recreated UDP socket");
            }
        } catch (Exception e) {
            logger.error("Socket recreation failed: {}", e.getMessage());
        }
    }

    public void stopListening() {
        listening = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
            try { listenerThread.join(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public AppNotification receiveNotification() throws IOException {
        byte[] buffer = receiveBuffer;
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
        try {
            clientSocket.receive(receivePacket);
            AppNotification notif = AppNotification.fromJson(new String(
                    receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8));
            
            if (notif != null && notif.getMessage() != null && !notif.getMessage().startsWith("REGISTER") && !notif.verifySignature()) {
                logger.warn("Notification rejetée (Signature HMAC invalide) : {}", notif.getId());
                return null;
            }
            return notif;
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (SocketException e) {
            if (e.getMessage() != null && e.getMessage().contains("buffer")) {
                return handleBufferOverflow();   // ← returns AppNotification now
            }
            throw e;
        }
    }

    // ── Fix: return type is AppNotification, not String ──────────────────────
    private AppNotification handleBufferOverflow() throws IOException {
        if (currentBufferSize >= AppConstants.MAX_BUFFER_SIZE)
            throw new IOException("Buffer already at maximum size: " + currentBufferSize);

        currentBufferSize = Math.min(currentBufferSize * 2, AppConstants.MAX_BUFFER_SIZE);
        receiveBuffer     = new byte[currentBufferSize];
        logger.info("Increased buffer size to: {}", currentBufferSize);

        return receiveNotification();   // ← retry with bigger buffer
    }

    private void dispatchToHandler(AppNotification notification) {
        if (notificationHandler == null || handlerExecutor == null) return;
        if (handlerExecutor.isShutdown()) return;
        handlerExecutor.submit(() -> {
            try { notificationHandler.accept(notification); }
            catch (Exception e) { logger.error("Handler error: {}", e.getMessage()); }
        });
    }

    private void closeSocket() {
        if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
    }

    public void close() {
        stopListening();
        closeSocket();
        shutdownExecutor();
    }

    private void shutdownExecutor() {
        if (handlerExecutor == null || handlerExecutor.isShutdown()) return;
        handlerExecutor.shutdown();
        try {
            if (!handlerExecutor.awaitTermination(5, TimeUnit.SECONDS))
                handlerExecutor.shutdownNow();
        } catch (InterruptedException e) {
            handlerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void setTimeout(int timeout) {
        try { clientSocket.setSoTimeout(timeout); }
        catch (SocketException e) { logger.error("Failed to set timeout: {}", e.getMessage()); }
    }

    public boolean isConnected()                          { return clientSocket != null && !clientSocket.isClosed() && listening; }
    public List<AppNotification> getReceivedNotifications() { return Collections.unmodifiableList(receivedNotifications); }
    public AppNotification getLastNotification()          { return receivedNotifications.isEmpty() ? null : receivedNotifications.get(receivedNotifications.size() - 1); }
    public int getReceivedCount()                         { return receivedNotifications.size(); }
    public void clearReceivedNotifications()              { receivedNotifications.clear(); }
}