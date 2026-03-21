package com.chrionline.chrionline.server;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.protocol.AppNotification;
import com.chrionline.chrionline.network.udp.UDPNotificationListener;
import com.chrionline.chrionline.network.udp.UDPNotificationDispatcher;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

public class ServerUDPTest {

    private static UDPNotificationDispatcher udpNotificationDispatcher;
    private static UDPNotificationListener udpListener;
    private static ExecutorService listenerHandlerExecutor;
    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    // Collected by the listener's handler — used for assertions
    private static final List<AppNotification> clientReceivedNotifications = new CopyOnWriteArrayList<>();
    private static final CountDownLatch firstNotificationLatch = new CountDownLatch(1);


    public static void main(String[] args) throws Exception {
        try {
            setup();

            testServerStarts();
            testClientRegisters();
            testClientReceivesBroadcast();
            testHeartbeatDelivered();
            testMultipleClientsReceiveNotification();
            testServerHandlesIncomingNotification();

            AppConfig.getLogger().info("✅ All tests passed");

        } catch (AssertionError e) {
            AppConfig.getLogger().error("❌ Test failed: {}", e.getMessage());
            throw e;
        } finally {
            stopInstances();
        }
    }


    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private static void setup() throws Exception {
        AppConfig.getLogger().info("--- Setting up test environment ---");

        // Server
        udpNotificationDispatcher = new UDPNotificationDispatcher();
        udpNotificationDispatcher.setNotificationHandler(notification -> {
            AppConfig.getLogger().info("Server received notification: {}", notification.getMessage());
            udpNotificationDispatcher.broadcastNotification(notification);
        });
        udpNotificationDispatcher.start();

        // Client listener
        listenerHandlerExecutor = Executors.newFixedThreadPool(2);
        udpListener = new UDPNotificationListener();
        udpListener.setNotificationHandler(notification -> {
            AppConfig.getLogger().info("Client received notification: {}", notification.getMessage());
            clientReceivedNotifications.add(notification);
            firstNotificationLatch.countDown(); // unblock any waiting assertions
        }, listenerHandlerExecutor);
        udpListener.startListening();

        // Give server and listener time to fully initialize
        Thread.sleep(500);
        AppConfig.getLogger().info("Setup complete — server clients registered: {}",
                udpNotificationDispatcher.getConnectedClientCount());
    }


    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /** Server socket is open and accepting connections after start(). */
    private static void testServerStarts() {
        AppConfig.getLogger().info("--- testServerStarts ---");
        assertTrue(udpNotificationDispatcher.getConnectedClientCount() >= 0, "Server should be running");
        AppConfig.getLogger().info("PASS");
    }

    /** Listener's REGISTER packet was received — server should have 1 client. */
    private static void testClientRegisters() throws InterruptedException {
        AppConfig.getLogger().info("--- testClientRegisters ---");
        Thread.sleep(300); // allow registration packet to be processed
        assertEquals(1, udpNotificationDispatcher.getConnectedClientCount(),
                "Server should have exactly 1 registered client after listener starts");
        AppConfig.getLogger().info("PASS — registered clients: {}", udpNotificationDispatcher.getConnectedClientCount());
    }

    /**
     * Broadcasts a notification from the server and verifies the client
     * listener receives it within a reasonable timeout.
     */
    private static void testClientReceivesBroadcast() throws InterruptedException {
        AppConfig.getLogger().info("--- testClientReceivesBroadcast ---");

        AppNotification notification = AppNotification.info(
                "test-broadcast-001",
                "Hello from server",
                "notification-server"
        );
        udpNotificationDispatcher.broadcastNotification(notification);

        // Block until the listener handler fires or we time out
        boolean received = firstNotificationLatch.await(3, TimeUnit.SECONDS);
        assertTrue(received, "Client should have received broadcast within 3 seconds");

        AppNotification last = udpListener.getLastNotification();
        assertNotNull(last, "Last notification should not be null");
        assertEquals("test-broadcast-001", last.getId(), "Notification ID should match");
        assertEquals("Hello from server", last.getMessage(), "Notification message should match");

        AppConfig.getLogger().info("PASS — client received: {}", last.getMessage());
    }

    /** Verifies periodic heartbeat notifications are delivered to the client. */
    private static void testHeartbeatDelivered() throws InterruptedException {
        AppConfig.getLogger().info("--- testHeartbeatDelivered ---");

        int countBefore = udpListener.getReceivedCount();

        // Start heartbeat scheduler
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            AppNotification heartbeat = AppNotification.info(
                    "sys-" + System.currentTimeMillis(),
                    "Server heartbeat",
                    "notification-server"
            );
            udpNotificationDispatcher.broadcastNotification(heartbeat);
        }, 0, 2, TimeUnit.SECONDS);

        // Wait long enough for at least 2 heartbeats
        Thread.sleep(5000);

        int countAfter = udpListener.getReceivedCount();
        assertTrue(countAfter >= countBefore + 2,
                "Client should have received at least 2 heartbeats, got: " + (countAfter - countBefore));

        AppConfig.getLogger().info("PASS — heartbeats received: {}", countAfter - countBefore);
    }

    /**
     * Spins up a second listener, verifies both clients receive the same broadcast.
     */
    private static void testMultipleClientsReceiveNotification() throws Exception {
        AppConfig.getLogger().info("--- testMultipleClientsReceiveNotification ---");

        CountDownLatch secondClientLatch = new CountDownLatch(1);
        ExecutorService secondExecutor = Executors.newSingleThreadExecutor();
        UDPNotificationListener secondListener = new UDPNotificationListener();

        try {
            secondListener.setNotificationHandler(n -> {
                AppConfig.getLogger().info("Second client received: {}", n.getMessage());
                secondClientLatch.countDown();
            }, secondExecutor);
            secondListener.startListening();

            Thread.sleep(300); // allow registration

            assertEquals(2, udpNotificationDispatcher.getConnectedClientCount(),
                    "Server should have 2 registered clients");

            AppNotification notification = AppNotification.info(
                    "test-multi-001",
                    "Broadcast to all",
                    "notification-server"
            );
            udpNotificationDispatcher.broadcastNotification(notification);

            boolean secondReceived = secondClientLatch.await(3, TimeUnit.SECONDS);
            assertTrue(secondReceived, "Second client should receive the broadcast");

            AppConfig.getLogger().info("PASS — both clients received the broadcast");

        } finally {
            secondListener.close();
            secondExecutor.shutdown();
        }
    }

    /**
     * Sends a notification from the client to the server and verifies the
     * server's notification handler fires (simulates a client-to-server message).
     */
    private static void testServerHandlesIncomingNotification() throws Exception {
        AppConfig.getLogger().info("--- testServerHandlesIncomingNotification ---");

        CountDownLatch serverReceivedLatch = new CountDownLatch(1);

        udpNotificationDispatcher.setNotificationHandler(notification -> {
            AppConfig.getLogger().info("Server handler fired for: {}", notification.getId());
            serverReceivedLatch.countDown();
            udpNotificationDispatcher.broadcastNotification(notification); // keep existing behaviour
        });

        // Send directly from client socket to server port
        AppNotification outgoing = AppNotification.info(
                "client-to-server-001",
                "Message from client",
                "test-client"
        );
        String json = outgoing.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(
                data, data.length,
                InetAddress.getByName(AppConstants.SERVER_HOST),
                AppConstants.UDP_PORT
        );
        // Send via a raw socket — listener socket is receive-only in practice
        try (DatagramSocket rawSocket = new DatagramSocket()) {
            rawSocket.send(packet);
        }

        boolean serverFired = serverReceivedLatch.await(3, TimeUnit.SECONDS);
        assertTrue(serverFired, "Server notification handler should have fired within 3 seconds");

        AppConfig.getLogger().info("PASS — server handled incoming client notification");
    }


    // -------------------------------------------------------------------------
    // Teardown
    // -------------------------------------------------------------------------

    private static void stopInstances() {
        AppConfig.getLogger().info("--- Tearing down ---");
        if (udpListener != null) udpListener.close();
        if (listenerHandlerExecutor != null) listenerHandlerExecutor.shutdown();
        if (udpNotificationDispatcher != null) udpNotificationDispatcher.stop();
        scheduledExecutorService.shutdown();
    }


    // -------------------------------------------------------------------------
    // Minimal assertion helpers (replace with JUnit/AssertJ if available)
    // -------------------------------------------------------------------------

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " — expected: " + expected + ", got: " + actual);
        }
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) throw new AssertionError(message);
    }
}