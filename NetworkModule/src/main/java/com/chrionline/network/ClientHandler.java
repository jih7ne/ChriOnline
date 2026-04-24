package com.chrionline.network;

import com.chrionline.security.core.SecureStreamWrapper;
import com.chrionline.security.core.SessionCipher;
import com.chrionline.core.constants.AppConstants;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.security.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ClientHandler extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket client;
    private BufferedReader input;
    private PrintWriter output;
    private final String clientId;
    private final String clientIp;   // IP extraite une seule fois à la construction
    private SessionCipher sessionCipher;
    private SecureStreamWrapper secureStream;

    public ClientHandler(Socket client) {
        this.client   = client;
        this.clientId = buildClientId(client);
        // On isole l'adresse IP (sans le port) pour le rate-limiting
        this.clientIp = client.getInetAddress().getHostAddress();

        try {
            client.setSoTimeout(AppConstants.SOCKET_TIMEOUT_MS);
            input  = new BufferedReader(new InputStreamReader(client.getInputStream(),  AppConstants.BUFFER_CHARSET));
            output = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), AppConstants.BUFFER_CHARSET), true);

            this.sessionCipher = ServerHandshake.perform(input, output, clientId);
            this.secureStream  = new SecureStreamWrapper(input, output, sessionCipher);
            logger.info("Client connecté : {}", clientId);
        } catch (IOException ioEx) {
            logger.error("Impossible d'initialiser le handler pour {}", clientId, ioEx);
            cleanup();
        } catch (Exception e) {
            logger.error("Handshake failed for {}", clientId, e);
            cleanup();
        }
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted() && !client.isClosed()) {
                try {
                    String message = secureStream.readLine();
                    if (message == null) {
                        logger.info("Client {} déconnecté normalement", clientId);
                        break;
                    }
                    processMessage(message);
                } catch (SocketTimeoutException e) {
                    logger.debug("Timeout de lecture pour {}", clientId);
                } catch (SocketException e) {
                    logger.info("Connexion fermée pour {} : {}", clientId, e.getMessage());
                    break;
                } catch (IOException e) {
                    logger.error("Erreur I/O pour {}", clientId, e);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Erreur inattendue dans le handler de {}", clientId, e);
        } finally {
            cleanup();
        }
    }

    private void processMessage(String message) {
        try {
            logger.info("Reçu de {} : {}", clientId, message);

            AppRequest request = AppRequest.fromJson(message);

            // ── Injection de l'IP client dans les headers ───────────────────
            // Le header "client-address" est exploité par AuthController pour
            // le rate-limiting par IP (LoginAttemptGuard).
            // On reconstruit la requête en ajoutant le header de manière propre.
            request = new AppRequest.Builder()
                    .controller(request.getController())
                    .action(request.getAction())
                    .payload(request.getPayload())
                    .parameters(request.getParameters())
                    .headers(request.getHeaders())
                    .authToken(request.getAuthToken())
                    .clientId(clientId)
                    .clientVersion(request.getClientVersion())
                    .id(request.getId())
                    .header("client-address", clientIp)
                    .build();

            String response = RequestDispatcher.dispatch(request);
            secureStream.writeLine(response);
            logger.info("Réponse envoyée à {}", clientId);

        } catch (Exception e) {
            logger.error("Erreur de traitement pour {}", clientId, e);
            output.println("{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private void cleanup() {
        try {
            if (input  != null) input.close();
            if (output != null) output.close();
            if (!client.isClosed()) client.close();
            logger.info("Client {} nettoyé", clientId);
        } catch (IOException e) {
            logger.error("Erreur lors du nettoyage de {}", clientId, e);
        }
    }

    private static String buildClientId(Socket socket) {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }
}