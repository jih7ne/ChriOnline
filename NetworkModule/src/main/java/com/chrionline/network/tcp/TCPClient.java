package com.chrionline.network.tcp;

import com.chrionline.core.constants.AppConstants;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.security.core.SecureStreamWrapper;
import com.chrionline.security.core.SessionCipher;
import com.chrionline.security.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

import static com.chrionline.network.utils.NetworkUtils.extractField;

public class TCPClient {

    private static final Logger logger = LoggerFactory.getLogger(TCPClient.class);

    private Socket client;
    private BufferedReader input;
    private PrintWriter output;
    private volatile boolean connected = false;
    private String authToken = null;
    private SecureStreamWrapper secureStream;

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
        try {
            secureStream.writeLine(request);
            logger.debug("→ [{}/{}]", extractField(request, "controller"), extractField(request, "action"));
            String response = secureStream.readLine();
            logger.debug("← [{}/{}] status={}", extractField(request, "controller"), extractField(request, "action"), extractField(response, "status"));
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
            logger.info("Client connected to {}:{}",
                    AppConstants.SERVER_HOST, AppConstants.SERVER_PORT);

            SessionCipher cipher = ClientHandshake.perform(input, output);
            this.secureStream    = new SecureStreamWrapper(input, output, cipher);
        } catch (IOException e) {
            logger.error("Unable to connect to server! {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            logger.info("Client disconnected");
        } catch (IOException e) {
            logger.error("Error disconnecting client", e);
        }
    }
}
