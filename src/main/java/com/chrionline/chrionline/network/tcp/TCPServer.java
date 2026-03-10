package com.chrionline.chrionline.network.tcp;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    private static ServerSocket serverSocket;

    public TCPServer() throws IOException {
        try{
            serverSocket = new ServerSocket(AppConstants.SERVER_PORT);
            AppConfig.getLogger().info("Server started on port {}", AppConstants.SERVER_PORT);
        }
        catch (IOException e){
            AppConfig.getLogger().error("\n Unable to set up port!{}", e.getMessage());
            System.exit(-1);
        }

        do{
            Socket clientSocket = serverSocket.accept();
            AppConfig.getLogger().info("New connection established!");
            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start();
        }while(true);
    }
}
