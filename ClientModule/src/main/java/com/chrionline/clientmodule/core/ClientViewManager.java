package com.chrionline.clientmodule.core;

import com.chrionline.shared.models.PanierProduit;
import com.chrionline.shared.models.Produit;

import java.util.List;
import java.util.Map;

public interface ClientViewManager {
    void showLoginView();
    void showRegisterView();
    void showCatalogueView(Map<String, Object> userData);
    void showPanierView(Map<String, Object> userData);
    void showDetailsProduit(Produit produit, Map<String, Object> userData);
    void showCheckoutView(Map<String, Object> userData, List<PanierProduit> panierItems);
    void showCheckoutViewForExisting(Map<String, Object> userData, List<PanierProduit> panierItems, int idCommande, String uuidCommande);
    void showConfirmationView(Map<String, Object> paiementData);
    void showConfirmationEchoueeView(Map<String, Object> userData, String messageErreur, Runnable onReessayer);
    void showHistoriqueCommandesView(Map<String, Object> userData);
    void showProfileView(Map<String, Object> userData);
    void showForgotPasswordView();
}
