package com.chrionline.chrionline.core.config;

import com.chrionline.chrionline.network.tcp.TCPClient;

import java.io.IOException;

public class ClientConfig {
    private static ClientConfig instance;
    private TCPClient tcpClient;

    private ClientConfig() {

    }

    public static synchronized ClientConfig getInstance() {
        if (instance == null) {
            instance = new ClientConfig();
        }
        return instance;
    }


    public void initialize() throws IOException {
        AppConfig.getLogger().info("Initializing client configuration...");
        this.tcpClient = new TCPClient();
        AppConfig.getLogger().info("Client configuration initialized successfully");
    }


    public TCPClient getTcpClient() {
        if (tcpClient == null) {
            throw new IllegalStateException("Client not initialized. Call initialize() first.");
        }
        return tcpClient;
    }


    public void shutdown() {
        AppConfig.getLogger().info("Shutting down client...");

        if (tcpClient != null) {
            tcpClient.disconnect();
            tcpClient = null;
        }
    }
}
