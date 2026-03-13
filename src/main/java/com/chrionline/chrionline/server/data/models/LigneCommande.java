package com.chrionline.chrionline.server.data.models;

public class LigneCommande {
    private int id;
    private int id_commande;
    private int id_produit;
    private int quantite;
    private double prix_unitaire;

    public LigneCommande() {
    }

    public LigneCommande(int id, int id_commande, int id_produit, int quantite, double prix_unitaire) {
        this.id = id;
        this.id_commande = id_commande;
        this.id_produit = id_produit;
        this.quantite = quantite;
        this.prix_unitaire = prix_unitaire;
    }

    public int getId() {
        return id;
    }

    public int getId_commande() {
        return id_commande;
    }

    public int getId_produit() {
        return id_produit;
    }

    public double getPrix_unitaire() {
        return prix_unitaire;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setId_commande(int id_commande) {
        this.id_commande = id_commande;
    }

    public void setId_produit(int id_produit) {
        this.id_produit = id_produit;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public void setPrix_unitaire(double prix_unitaire) {
        this.prix_unitaire = prix_unitaire;
    }
}
