package com.chrionline.clientmodule.utils;

import com.chrionline.core.constants.AppConstants;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class CaptchaServer {

    private static final int PORT = 8765;
    private static HttpServer server;

    public static int start() throws Exception {
        if (server != null) return PORT; // déjà démarré

        server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);

        server.createContext("/recaptcha", exchange -> {
            String html = buildHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.setExecutor(null);
        server.start();
        return PORT;
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private static String buildHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <script src="https://www.google.com/recaptcha/api.js" async defer></script>
              <style>
                * { margin: 0; padding: 0; }
                body {
                  background: transparent;
                  display: flex;
                  justify-content: center;
                  padding-top: 4px;
                }
              </style>
            </head>
            <body>
              <div class="g-recaptcha"
                   data-sitekey="%s"
                   data-callback="onSuccess"
                   data-expired-callback="onExpired">
              </div>
              <script>
                function onSuccess(token) {
                  if (window.javabridge) window.javabridge.onTokenReceived(token);
                }
                function onExpired() {
                  if (window.javabridge) window.javabridge.onTokenExpired();
                }
              </script>
            </body>
            </html>
            """.formatted(AppConstants.RECAPTCHA_SITE_KEY);
    }
}