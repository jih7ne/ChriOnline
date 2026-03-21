package com.chrionline.chrionline.network.udp;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.protocol.AppNotification;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UDPServer {
    private DatagramSocket serverSocket;
    private volatile boolean running = false;
    private Thread serverThread;
    private ExecutorService clientHandlerPool;
    private byte[] receiveBuffer;
    private Consumer<AppNotification> notificationHandler;

    // For broadcasting to multiple clients
    private final List<ClientInfo> clients = new CopyOnWriteArrayList<>();

    public UDPServer() throws SocketException {
        initializeSocket();
    }

    private void initializeSocket() throws SocketException {
        this.serverSocket = new DatagramSocket(AppConstants.UDP_PORT);
        this.clientHandlerPool = Executors.newCachedThreadPool();
        this.receiveBuffer = new byte[AppConstants.BUFFER_SIZE];

        AppConfig.getLogger().info("UDP Server initialized on port {}", AppConstants.UDP_PORT);
    }

    /**
     * Start the UDP server
     */
    public void start() {
        if (running) {
            AppConfig.getLogger().warn("Server already running");
            return;
        }

        running = true;
        serverThread = new Thread(this::run, "UDP-Server-Main");
        serverThread.start();

        AppConfig.getLogger().info("UDP Server started");
    }

    private void run() {
        while (running) {
            try {

                DatagramPacket receivePacket = new DatagramPacket(
                        receiveBuffer,
                        receiveBuffer.length
                );

                serverSocket.receive(receivePacket);


                clientHandlerPool.submit(() -> handleClientPacket(receivePacket));

            } catch (SocketException e) {
                if (running) {
                    AppConfig.getLogger().error("Socket error: {}", e.getMessage());
                }
            } catch (IOException e) {
                if (running) {
                    AppConfig.getLogger().error("IO error: {}", e.getMessage());
                }
            }
        }
    }

    private void handleClientPacket(DatagramPacket packet) {
        try {

            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();


            String receivedJson = new String(
                    packet.getData(),
                    0,
                    packet.getLength(),
                    StandardCharsets.UTF_8
            );


            try {
                AppNotification notification = AppNotification.fromJson(receivedJson);


                if (notificationHandler != null) {
                    notificationHandler.accept(notification);
                }

                AppConfig.getLogger().debug("Received notification from {}:{} - Type: {}",
                        clientAddress, clientPort, notification.getType());

            } catch (Exception e) {
                // Not a valid notification - might be a client registration
                handleClientRegistration(receivedJson, clientAddress, clientPort);
            }

        } catch (Exception e) {
            AppConfig.getLogger().error("Error handling client packet: {}", e.getMessage());
        }
    }

    private void handleClientRegistration(String message, InetAddress address, int port) {
        if ("REGISTER".equals(message)) {
            clients.add(new ClientInfo(address, port, System.currentTimeMillis()));
            AppConfig.getLogger().info("Client registered: {}:{}", address, port);
        }
    }

    /**
     * Send a notification to a specific client
     */
    public void sendNotification(AppNotification notification, InetAddress address, int port)
            throws IOException {
        String json = notification.toJson();
        byte[] sendData = json.getBytes(StandardCharsets.UTF_8);

        DatagramPacket packet = new DatagramPacket(
                sendData,
                sendData.length,
                address,
                port
        );

        serverSocket.send(packet);
        AppConfig.getLogger().debug("Sent notification to {}:{}", address, port);
    }

    /**
     * Broadcast notification to all registered clients
     */
    public void broadcastNotification(AppNotification notification) {
        String json = notification.toJson();
        byte[] sendData = json.getBytes(StandardCharsets.UTF_8);

        for (ClientInfo client : clients) {
            try {
                DatagramPacket packet = new DatagramPacket(
                        sendData,
                        sendData.length,
                        client.address,
                        client.port
                );

                serverSocket.send(packet);

            } catch (IOException e) {
                AppConfig.getLogger().error("Failed to send to {}:{} - {}",
                        client.address, client.port, e.getMessage());
                // Remove dead clients? Mark for cleanup?
            }
        }

        AppConfig.getLogger().debug("Broadcast notification to {} clients", clients.size());
    }

    /**
     * Set handler for received notifications
     */
    public void setNotificationHandler(Consumer<AppNotification> handler) {
        this.notificationHandler = handler;
    }

    /**
     * Stop the UDP server
     */
    public void stop() {
        running = false;

        if (serverThread != null) {
            serverThread.interrupt();
            try {
                serverThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (clientHandlerPool != null) {
            clientHandlerPool.shutdown();
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        clients.clear();
        AppConfig.getLogger().info("UDP Server stopped");
    }

    /**
     * Get number of connected clients
     */
    public int getConnectedClientCount() {
        return clients.size();
    }

    // Simple client info class
    private static class ClientInfo {
        final InetAddress address;
        final int port;
        final long registeredAt;

        ClientInfo(InetAddress address, int port, long registeredAt) {
            this.address = address;
            this.port = port;
            this.registeredAt = registeredAt;
        }
    }
}
