package com.chrionline.chrionline.server.data.models;

public class PanierProduit {
    private int idPanier;
    private int idProduit;
    private int quantite;

    // infos du produit pour affichage
    private String nomProduit;
    private double prix;
    private String urlImage;

    public PanierProduit() {}

    public PanierProduit(int idPanier, int idProduit, int quantite) {
        this.idPanier = idPanier;
        this.idProduit = idProduit;
        this.quantite = quantite;
    }

    // sous-total de cette ligne
    public double getSousTotal() {
        return prix * quantite;
    }

    public int getIdPanier() { return idPanier; }
    public void setIdPanier(int idPanier) { this.idPanier = idPanier; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public String getUrlImage() { return urlImage; }
    public void setUrlImage(String urlImage) { this.urlImage = urlImage; }
}