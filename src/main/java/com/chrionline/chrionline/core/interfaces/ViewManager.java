package com.chrionline.chrionline.core.interfaces;

import com.chrionline.chrionline.server.data.models.Produit;
import java.util.Map;

public interface ViewManager {
    void showLoginView();
    void showRegisterView();
    void showCatalogueView(Map<String, Object> userData);
    void showPanierView(Map<String, Object> userData);
    void showDetailsProduit(Produit produit, Map<String, Object> userData);
    void showAdminView(Map<String, Object> userData);
}