package com.chrionline.chrionline.server.data.dto;

/**
 * Classes de désérialisation Gson pour AuthController.
 * Doivent être publiques pour que Gson puisse y accéder via réflexion
 * dans un contexte de modules Java (module-info.java).
 */

public class AuthPayloads {

    public static class LoginPayload {
        public String email;
        public String password;
    }

    public static class RegisterPayload {
        public String nom;
        public String prenom;
        public String email;
        public String password;
    }

    public static class UpdateProfilPayload {
        public String nom;
        public String prenom;
        public String email;
    }

    public static class UpdatePasswordPayload {
        public String ancien;
        public String nouveau;
    }

    public static class IdPayload {
        public int id;
    }
}