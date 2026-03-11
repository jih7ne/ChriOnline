package com.chrionline.chrionline.network.tcp;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;

import java.io.*;
import java.net.Socket;

public class TCPClient {

    private Socket client;
    private BufferedReader input;
    private PrintWriter output;
    private volatile boolean connected = false;
    private String authToken = null;

    public TCPClient() throws IOException {
        connect();
    }


    public String getAuthToken() { return authToken; }
    public void setAuthToken(String token) { this.authToken = token; }
    public boolean isAuthenticated() { return authToken != null && !authToken.isEmpty(); }

    public String sendRequest(String request) throws IOException {
        if (!connected || client.isClosed()) {
            throw new IOException("Client not connected");
        }
        output.println(request);
        AppConfig.getLogger().debug("Sent request: {}", request);
        String response = input.readLine();
        AppConfig.getLogger().debug("Received response: {}", response);
        return response;
    }

    public String sendRequest(AppRequest request) throws IOException {
        return sendRequest(request.toJson());
    }

    public AppResponse sendAndParse(String request) throws IOException {
        String response = sendRequest(request);
        return AppResponse.fromJson(response);
    }

    public AppResponse sendAndParse(AppRequest request) throws IOException {
        String response = sendRequest(request);
        return AppResponse.fromJson(response);
    }

    public <T> T sendAndGetData(String request, Class<T> dataType) throws IOException {
        AppResponse response = sendAndParse(request);
        if (!response.isSuccess()) {
            throw new RuntimeException("Request failed: " + response.getMessage());
        }
        return response.getDataAs(dataType);
    }

    private void connect() throws IOException {
        try {
            client = new Socket(AppConstants.SERVER_HOST, AppConstants.SERVER_PORT);
            input = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), AppConstants.BUFFER_CHARSET)
            );
            output = new PrintWriter(
                    new OutputStreamWriter(client.getOutputStream(), AppConstants.BUFFER_CHARSET),
                    true
            );
            connected = true;
            AppConfig.getLogger().info("Client connected to {}:{}",
                    AppConstants.SERVER_HOST, AppConstants.SERVER_PORT);
        } catch (IOException e) {
            AppConfig.getLogger().error("Unable to connect to server! {}", e.getMessage());
            throw e;
        }
    }

    public boolean isConnected() {
        return connected && client != null && client.isConnected() && !client.isClosed();
    }

    public void disconnect() {
        connected = false;
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (client != null && !client.isClosed()) client.close();
            AppConfig.getLogger().info("Client disconnected");
        } catch (IOException e) {
            AppConfig.getLogger().error("Error disconnecting client", e);
        }
    }
}
