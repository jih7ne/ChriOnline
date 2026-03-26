package com.chrionline.network;


import com.chrionline.core.constants.AppConstants;
import com.chrionline.network.protocol.AppRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;



public class ClientHandler extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
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

            logger.info("Client connected: {}", clientId);

        } catch (IOException ioEx) {
            logger.error("Failed to initialize client handler for {}", clientId, ioEx);
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

                        logger.info("Client {} disconnected normally", clientId);
                        break;
                    }

                    processMessage(message);

                } catch (SocketTimeoutException e) {

                    logger.debug("Read timeout for client {}", clientId);
                    continue;

                } catch (SocketException e) {

                    logger.info("Client {} connection closed: {}", clientId, e.getMessage());
                    break;

                } catch (IOException e) {

                    logger.error("IO error for client {}", clientId, e);
                    break;
                }
            }

        } catch (Exception e) {
            logger.error("Unexpected error in client handler for {}", clientId, e);
        } finally {
            cleanup();
        }
    }

    private void processMessage(String message) {
        try {
            logger.info("Client {} - Received: {}", clientId, message);


            String response = RequestDispatcher.dispatch(AppRequest.fromJson(message));


            output.println(response);

            logger.info("Client {} - Sent response", clientId);

        } catch (Exception e) {
            logger.error("Error processing request from client {}", clientId, e);
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
            logger.info("Client {} cleaned up", clientId);
        } catch (IOException e) {
            logger.error("Error during cleanup for client {}", clientId, e);
        }
    }

    private String generateClientId() {
        return String.format("%s:%d",
                client.getInetAddress().getHostAddress(),
                client.getPort());
    }
}
