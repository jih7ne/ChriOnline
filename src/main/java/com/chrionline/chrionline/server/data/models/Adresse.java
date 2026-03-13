package com.chrionline.chrionline.server.data.models;

public class Adresse {
    private int id;
    private int id_utilisateur;
    private String rue;
    private String complement;
    private String ville;
    private String code_postal;
    private String pays;
    private Boolean est_principale;

    public Adresse() {
    }

    public Adresse(int id, int id_utilisateur, String rue, String complement, String ville, String code_postal, String pays, Boolean est_principale) {
        this.id = id;
        this.id_utilisateur = id_utilisateur;
        this.rue = rue;
        this.complement = complement;
        this.ville = ville;
        this.code_postal = code_postal;
        this.pays = pays;
        this.est_principale = est_principale;
    }

    public int getId() {
        return id;
    }

    public int getId_utilisateur() {
        return id_utilisateur;
    }

    public String getRue() {
        return rue;
    }

    public String getComplement() {
        return complement;
    }

    public String getVille() {
        return ville;
    }

    public String getCode_postal() {
        return code_postal;
    }

    public String getPays() {
        return pays;
    }

    public Boolean getEst_principale() {
        return est_principale;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setId_utilisateur(int id_utilisateur) {
        this.id_utilisateur = id_utilisateur;
    }

    public void setRue(String rue) {
        this.rue = rue;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public void setCode_postal(String code_postal) {
        this.code_postal = code_postal;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public void setEst_principale(Boolean est_principale) {
        this.est_principale = est_principale;
    }
}
