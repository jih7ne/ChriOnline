package com.chrionline.chrionline.network.udp;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.protocol.AppNotification;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UDPNotificationDispatcher {

    private DatagramSocket serverSocket;
    private volatile boolean running = false;
    private Thread serverThread;
    private ExecutorService clientHandlerPool;
    private byte[] receiveBuffer;
    private Consumer<AppNotification> notificationHandler;

    /**
     * Anonymous clients (registered without a userId).
     * Used for broadcast.
     */
    private final List<ClientInfo> clients = new CopyOnWriteArrayList<>();

    /**
     * Named clients: userId → ClientInfo.
     * Populated when a client sends a REGISTER packet that includes a userId.
     * Used for targeted delivery.
     */
    private final Map<Integer, ClientInfo> userClients = new ConcurrentHashMap<>();

    public UDPNotificationDispatcher() throws SocketException {
        initializeSocket();
    }

    private void initializeSocket() throws SocketException {
        this.serverSocket    = new DatagramSocket(AppConstants.UDP_PORT);
        this.clientHandlerPool = Executors.newCachedThreadPool();
        this.receiveBuffer   = new byte[AppConstants.BUFFER_SIZE];
        AppConfig.getLogger().info("UDP Server initialized on port {}", AppConstants.UDP_PORT);
    }

    public void start() {
        if (running) { AppConfig.getLogger().warn("Server already running"); return; }
        running = true;
        serverThread = new Thread(this::run, "UDP-Server-Main");
        serverThread.start();
        AppConfig.getLogger().info("UDP Server started");
    }

    private void run() {
        while (running) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);
                clientHandlerPool.submit(() -> handleClientPacket(receivePacket));
            } catch (SocketException e) {
                if (running) AppConfig.getLogger().error("Socket error: {}", e.getMessage());
            } catch (IOException e) {
                if (running) AppConfig.getLogger().error("IO error: {}", e.getMessage());
            }
        }
    }

    private void handleClientPacket(DatagramPacket packet) {
        try {
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            String receivedJson = new String(
                    packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

            try {
                AppNotification notification = AppNotification.fromJson(receivedJson);
                String msg = notification.getMessage();

                if (msg != null && msg.trim().startsWith("REGISTER")) {
                    // Format: "REGISTER" or "REGISTER:userId"
                    if (msg.contains(":")) {
                        try {
                            int userId = Integer.parseInt(msg.trim().split(":")[1]);
                            handleUserRegistration(clientAddress, clientPort, userId);
                        } catch (NumberFormatException ex) {
                            handleAnonymousRegistration(clientAddress, clientPort);
                        }
                    } else {
                        handleAnonymousRegistration(clientAddress, clientPort);
                    }
                    return;
                }

                if (notificationHandler != null) {
                    notificationHandler.accept(notification);
                }

            } catch (Exception e) {
                AppConfig.getLogger().warn("Unrecognised packet from {}:{} — {}",
                        clientAddress, clientPort, receivedJson);
            }
        } catch (Exception e) {
            AppConfig.getLogger().error("Error handling client packet: {}", e.getMessage());
        }
    }

    private void handleAnonymousRegistration(InetAddress address, int port) {
        clients.removeIf(c -> c.address.equals(address) && c.port == port);
        clients.add(new ClientInfo(address, port, System.currentTimeMillis()));
        AppConfig.getLogger().info("[UDP] Anonymous client registered: {}:{} — total: {}",
                address, port, clients.size());
    }

    private void handleUserRegistration(InetAddress address, int port, int userId) {
        // Also keep in anonymous list for broadcast
        clients.removeIf(c -> c.address.equals(address) && c.port == port);
        clients.add(new ClientInfo(address, port, System.currentTimeMillis()));

        // Register in user map
        userClients.put(userId, new ClientInfo(address, port, System.currentTimeMillis()));
        AppConfig.getLogger().info("[UDP] User {} registered: {}:{} — named clients: {}",
                userId, address, port, userClients.size());
    }

    // ─── Send to a specific user ───────────────────────────────────────────────

    /**
     * Send a notification to a specific user by their userId.
     * Falls back to broadcast if the user is not registered.
     *
     * @return true if sent to the specific user, false if fell back to broadcast
     */
    public boolean sendToUser(int userId, AppNotification notification) {
        ClientInfo target = userClients.get(userId);
        if (target != null) {
            try {
                String json = notification.toJson();
                byte[] sendData = json.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(
                        sendData, sendData.length, target.address, target.port);
                serverSocket.send(packet);
                AppConfig.getLogger().debug("Sent notification to user {} at {}:{}",
                        userId, target.address, target.port);
                return true;
            } catch (IOException e) {
                AppConfig.getLogger().error("Failed to send to user {}: {}", userId, e.getMessage());
                userClients.remove(userId);
            }
        }
        // User not connected via UDP — silently ignore (don't broadcast to everyone)
        AppConfig.getLogger().warn("User {} not registered for UDP — notification not delivered", userId);
        return false;
    }

    // ─── Broadcast to all ─────────────────────────────────────────────────────

    public void broadcastNotification(AppNotification notification) {
        String json = notification.toJson();
        byte[] sendData = json.getBytes(StandardCharsets.UTF_8);
        List<ClientInfo> deadClients = new ArrayList<>();

        for (ClientInfo client : clients) {
            try {
                DatagramPacket packet = new DatagramPacket(
                        sendData, sendData.length, client.address, client.port);
                serverSocket.send(packet);
            } catch (IOException e) {
                AppConfig.getLogger().error("Failed to send to {}:{}", client.address, client.port);
                deadClients.add(client);
            }
        }

        if (!deadClients.isEmpty()) {
            clients.removeAll(deadClients);
        }

        AppConfig.getLogger().debug("Broadcast notification to {} clients", clients.size());
    }

    public void setNotificationHandler(Consumer<AppNotification> handler) {
        this.notificationHandler = handler;
    }

    public void stop() {
        running = false;
        if (serverThread != null) {
            serverThread.interrupt();
            try { serverThread.join(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if (clientHandlerPool != null) clientHandlerPool.shutdown();
        if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        clients.clear();
        userClients.clear();
        AppConfig.getLogger().info("UDP Server stopped");
    }

    public int getConnectedClientCount() { return clients.size(); }

    private static class ClientInfo {
        final InetAddress address;
        final int port;
        final long registeredAt;
        ClientInfo(InetAddress address, int port, long registeredAt) {
            this.address      = address;
            this.port         = port;
            this.registeredAt = registeredAt;
        }
    }
}