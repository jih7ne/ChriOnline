package com.chrionline.chrionline.network;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.protocol.AppRequest;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ClientHandler extends Thread {
    private Socket client;
    private BufferedReader input;
    private PrintWriter output;
    private String clientId;


    public ClientHandler(Socket client) {
        this.client = client;
        this.clientId = generateClientId();



        try {

            client.setSoTimeout(AppConstants.SOCKET_TIMEOUT_MS);


            input = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), AppConstants.BUFFER_CHARSET)
            );


            output = new PrintWriter(
                    new OutputStreamWriter(client.getOutputStream(), AppConstants.BUFFER_CHARSET),
                    true
            );

            AppConfig.getLogger().info("Client connected: {}", clientId);

        } catch (IOException ioEx) {
            AppConfig.getLogger().error("Failed to initialize client handler for {}", clientId, ioEx);
            cleanup();
        }
    }

    public void run() {
        try {
            String message;


            while (!isInterrupted() && !client.isClosed()) {
                try {
                    message = input.readLine();

                    if (message == null) {

                        AppConfig.getLogger().info("Client {} disconnected normally", clientId);
                        break;
                    }

                    processMessage(message);

                } catch (SocketTimeoutException e) {

                    AppConfig.getLogger().debug("Read timeout for client {}", clientId);
                    continue;

                } catch (SocketException e) {

                    AppConfig.getLogger().info("Client {} connection closed: {}", clientId, e.getMessage());
                    break;

                } catch (IOException e) {

                    AppConfig.getLogger().error("IO error for client {}", clientId, e);
                    break;
                }
            }

        } catch (Exception e) {
            AppConfig.getLogger().error("Unexpected error in client handler for {}", clientId, e);
        } finally {
            cleanup();
        }
    }

    private void processMessage(String message) {
        try {
            AppConfig.getLogger().info("Client {} - Received: {}", clientId, message);


            String response = RequestDispatcher.dispatch(AppRequest.fromJson(message));


            output.println(response);

            AppConfig.getLogger().info("Client {} - Sent response", clientId);

        } catch (Exception e) {
            AppConfig.getLogger().error("Error processing request from client {}", clientId, e);
            output.println("{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private void cleanup() {
        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (client != null && !client.isClosed()) {
                client.close();
            }
            AppConfig.getLogger().info("Client {} cleaned up", clientId);
        } catch (IOException e) {
            AppConfig.getLogger().error("Error during cleanup for client {}", clientId, e);
        }
    }

    private String generateClientId() {
        return String.format("%s:%d",
                client.getInetAddress().getHostAddress(),
                client.getPort());
    }
}
