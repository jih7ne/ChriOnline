package com.chrionline.chrionline.network;

import com.chrionline.chrionline.core.config.AppConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler extends Thread{
    private Socket client;
    private Scanner input;
    private PrintWriter output;

    public ClientHandler(Socket client){
        this.client = client;

        try{
            input = new Scanner(client.getInputStream());
            output = new PrintWriter(client.getOutputStream(), true);
        }
        catch(IOException ioEx){
            AppConfig.getLogger().error(ioEx.getMessage(), ioEx);
        }
    }

    public void run(){
        try {
            while(true){
                if (input.hasNextLine()) {
                    String message = input.nextLine();
                    AppConfig.getLogger().info("Received request: {}", message);

                    try {

                        String response = RequestDispatcher.dispatch(message);


                        output.println(response);
                        output.flush();

                        AppConfig.getLogger().info("Sent response: {}", response);

                    } catch (Exception e) {
                        AppConfig.getLogger().error("Error processing request", e);
                        output.println("ERROR: " + e.getMessage());
                        output.flush();
                    }
                }
            }
        } catch (Exception e) {
            AppConfig.getLogger().error("Client handler error", e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                AppConfig.getLogger().error("Error closing client connection", e);
            }
        }
    }
}
