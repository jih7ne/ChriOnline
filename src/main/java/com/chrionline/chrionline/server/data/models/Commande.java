package com.chrionline.chrionline.server.data.models;

import com.chrionline.chrionline.core.enums.StatutCommande;

import java.time.LocalDateTime;

public class Commande {
    private int id_commande;
    private String uuid_commande;
    private int id_utilisateur;
    private Integer id_panier;
    private int id_adresse;
    private LocalDateTime date;
    private StatutCommande statut;
    private double prix_total;

    public Commande() {
    }

    public Commande(int id_commande, String uuid_commande, int id_utilisateur, Integer id_panier, int id_adresse, LocalDateTime date, StatutCommande statut, double prix_total) {
        this.id_commande = id_commande;
        this.uuid_commande = uuid_commande;
        this.id_utilisateur = id_utilisateur;
        this.id_panier = id_panier;
        this.id_adresse = id_adresse;
        this.date = date;
        this.statut = statut;
        this.prix_total = prix_total;
    }

    public int getId_commande() {
        return id_commande;
    }

    public String getUuid_commande() {
        return uuid_commande;
    }

    public int getId_utilisateur() {
        return id_utilisateur;
    }

    public Integer getId_panier() {
        return id_panier;
    }

    public int getId_adresse() {
        return id_adresse;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public StatutCommande getStatut() {
        return statut;
    }

    public double getPrix_total() {
        return prix_total;
    }

    public void setId_commande(int id_commande) {
        this.id_commande = id_commande;
    }

    public void setUuid_commande(String uuid_commande) {
        this.uuid_commande = uuid_commande;
    }

    public void setId_utilisateur(int id_utilisateur) {
        this.id_utilisateur = id_utilisateur;
    }

    public void setId_panier(Integer id_panier) {
        this.id_panier = id_panier;
    }

    public void setId_adresse(int id_adresse) {
        this.id_adresse = id_adresse;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    public void setPrix_total(double prix_total) {
        this.prix_total = prix_total;
    }
}
