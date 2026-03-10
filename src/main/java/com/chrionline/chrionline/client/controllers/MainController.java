package com.chrionline.chrionline.client.controllers;

import com.chrionline.chrionline.core.config.ClientConfig;
import com.chrionline.chrionline.core.interfaces.ConfigAware;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.tcp.TCPClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController implements ConfigAware {

    private ClientConfig clientConfig;
    private ViewManager viewManager;
    private TCPClient tcpClient;

    @Override
    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.tcpClient = clientConfig.getTcpClient();
    }

    @Override
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }


    public void sendDummyRequest() throws IOException {

        Map<String, String> map = new HashMap<>();
        map.put("password", "123");
        map.put("email", "1@11.com");
        map.put("username", "admin");

        AppRequest appRequest = new AppRequest.Builder()
                .controller("Test")
                .action("test")
                .payload(JsonUtils.toJson(map))
                .build();

        System.out.println(tcpClient.sendRequest(appRequest));
    }
}
