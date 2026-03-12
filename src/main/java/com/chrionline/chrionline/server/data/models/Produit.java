package com.chrionline.chrionline.server.data.models;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private double prix;
    private int stock;
    private String urlImage;
    private int idCategorie;
    private String nomCategorie; // pour affichage (JOIN)

    public Produit() {}

    public Produit(String nom, String description, double prix,
                   int stock, String urlImage, int idCategorie) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.stock = stock;
        this.urlImage = urlImage;
        this.idCategorie = idCategorie;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getUrlImage() { return urlImage; }
    public void setUrlImage(String urlImage) { this.urlImage = urlImage; }

    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }
}