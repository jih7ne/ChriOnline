package com.chrionline.chrionline.core.utils;

import com.chrionline.chrionline.client.ui.components.ClientNavbar;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.server.data.models.PanierProduit;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class PanierUtils {

    /**
     * Charge le nombre d'articles dans le panier depuis le serveur TCP
     * et met à jour le compteur de la navbar automatiquement.
     *
     * À appeler dans buildUI() de chaque vue après la création de la navbar.
     *
     * Exemple d'utilisation :
     *   PanierUtils.chargerCartCount(client, userData, navbar);
     */
    public static void chargerCartCount(TCPClient client,
                                        Map<String, Object> userData,
                                        ClientNavbar navbar) {
        new Thread(() -> {
            try {
                int idUtilisateur = ((Double) userData.get("id")).intValue();
                AppRequest request = new AppRequest.Builder()
                        .controller("Panier")
                        .action("getPanier")
                        .parameter("idUtilisateur", idUtilisateur)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        Type type = new TypeToken<List<PanierProduit>>(){}.getType();
                        List<PanierProduit> items = new Gson().fromJson(
                                new Gson().toJson(response.getData()), type
                        );
                        int count = (items != null) ? items.size() : 0;
                        navbar.updateCartCount(count);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}