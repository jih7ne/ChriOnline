package com.chrionline.chrionline.server.data.models;

import com.chrionline.chrionline.core.enums.MethodePaiement;
import com.chrionline.chrionline.core.enums.StatutPaiement;

import java.time.LocalDateTime;

public class Paiement {
    private int id;
    private int id_commande;
    private LocalDateTime date_paiement;
    private MethodePaiement methode_paiement;
    private StatutPaiement statut;
    private String numero_masque;

    public Paiement() {
    }

    public Paiement(int id, int id_commande, LocalDateTime date_paiement, MethodePaiement methode_paiement, StatutPaiement statut, String numero_masque) {
        this.id = id;
        this.id_commande = id_commande;
        this.date_paiement = date_paiement;
        this.methode_paiement = methode_paiement;
        this.statut = statut;
        this.numero_masque = numero_masque;
    }

    public int getId() {
        return id;
    }

    public int getId_commande() {
        return id_commande;
    }

    public LocalDateTime getDate_paiement() {
        return date_paiement;
    }

    public MethodePaiement getMethode_paiement() {
        return methode_paiement;
    }

    public StatutPaiement getStatut() {
        return statut;
    }

    public String getNumero_masque() {
        return numero_masque;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setId_commande(int id_commande) {
        this.id_commande = id_commande;
    }

    public void setDate_paiement(LocalDateTime date_paiement) {
        this.date_paiement = date_paiement;
    }

    public void setMethode_paiement(MethodePaiement methode_paiement) {
        this.methode_paiement = methode_paiement;
    }

    public void setStatut(StatutPaiement statut) {
        this.statut = statut;
    }

    public void setNumero_masque(String numero_masque) {
        this.numero_masque = numero_masque;
    }
}
