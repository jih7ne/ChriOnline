package com.chrionline.network.tcp;


import com.chrionline.core.constants.AppConstants;
import com.chrionline.network.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    private static final Logger logger = LoggerFactory.getLogger(TCPServer.class);
    private static ServerSocket serverSocket;

    public TCPServer() throws IOException {
        try{
            serverSocket = new ServerSocket(AppConstants.SERVER_PORT);
            logger.info("Server started on port {}", AppConstants.SERVER_PORT);
        }
        catch (IOException e){
            logger.error("\n Unable to set up port!{}", e.getMessage());
            System.exit(-1);
        }

        do{
            Socket clientSocket = serverSocket.accept();
            logger.info("New connection established!");
            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start();
        }while(true);
    }
}
