package com.chrionline.chrionline.server.data.models;

public class Utilisateur {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String role;
    private String statut;
    private String questionSecrete;
    private String reponseSecrete;

    public Utilisateur() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getQuestionSecrete() { return questionSecrete; }
    public void setQuestionSecrete(String questionSecrete) { this.questionSecrete = questionSecrete; }

    public String getReponseSecrete() { return reponseSecrete; }
    public void setReponseSecrete(String reponseSecrete) { this.reponseSecrete = reponseSecrete; }
}