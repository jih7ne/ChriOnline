package com.chrionline.server.data.dto;

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

    public static class TwoFactorVerifyPayload {
        public String tempToken; // token temporaire après login
        public String code;      // code 6 chiffres saisi par l'utilisateur
    }

    public static class TwoFactorEnablePayload {
        public String code; // premier code pour confirmer l'activation
    }
}
