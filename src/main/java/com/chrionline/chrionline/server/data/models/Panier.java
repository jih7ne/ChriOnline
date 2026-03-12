package com.chrionline.chrionline.server.data.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Panier {
    private int id;
    private int idUtilisateur;
    private LocalDateTime dateCreation;
    private List<PanierProduit> produits; // liste des produits du panier

    public Panier() {
        this.produits = new ArrayList<>();
    }

    public Panier(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
        this.produits = new ArrayList<>();
    }

    // calcul automatique du total
    public double getTotal() {
        return produits.stream()
                .mapToDouble(pp -> pp.getPrix() * pp.getQuantite())
                .sum();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public List<PanierProduit> getProduits() { return produits; }
    public void setProduits(List<PanierProduit> produits) { this.produits = produits; }
}