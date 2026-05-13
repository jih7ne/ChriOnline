package com.chrionline.server.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PasswordValidator {

    public static final int    MIN_LENGTH    = 8;
    public static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;',.<>?/`~";

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password123", "123456", "12345678",
            "123456789", "1234567890", "000000", "111111", "qwerty",
            "azerty", "abc123", "iloveyou", "admin", "letmein",
            "welcome", "monkey", "dragon", "master", "superman",
            "batman", "trustno1", "passw0rd", "p@ssword", "p@ss1234",
            "soleil", "bonjour", "chocolat", "football", "jordan23"
    );


    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }
        public String firstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }
    }



    public static ValidationResult validate(String password) {
        return validate(password, null, null);
    }

    public static ValidationResult validate(String password, String prenom, String nom) {
        if (password == null) {
            return new ValidationResult(false, List.of("Le mot de passe est requis."));
        }

        List<String> errors = new ArrayList<>();



        if (password.length() < MIN_LENGTH) {
            errors.add("Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères.");
        }

        if (!hasUppercase(password)) {
            errors.add("Le mot de passe doit contenir au moins une lettre majuscule.");
        }

        if (!hasLowercase(password)) {
            errors.add("Le mot de passe doit contenir au moins une lettre minuscule.");
        }

        if (!hasDigit(password)) {
            errors.add("Le mot de passe doit contenir au moins un chiffre.");
        }

        if (!hasSpecialChar(password)) {
            errors.add("Le mot de passe doit contenir au moins un caractère spécial (" + SPECIAL_CHARS + ").");
        }



        String lower = password.toLowerCase();

        if (prenom != null && !prenom.isBlank()) {
            String prenomLower = prenom.trim().toLowerCase();
            if (prenomLower.length() >= 3 && lower.contains(prenomLower)) {
                errors.add("Le mot de passe ne doit pas contenir votre prénom.");
            }
        }

        if (nom != null && !nom.isBlank()) {
            String nomLower = nom.trim().toLowerCase();
            if (nomLower.length() >= 3 && lower.contains(nomLower)) {
                errors.add("Le mot de passe ne doit pas contenir votre nom.");
            }
        }


        if (COMMON_PASSWORDS.contains(lower)) {
            errors.add("Ce mot de passe est trop courant. Choisissez-en un plus original.");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static boolean hasUppercase(String s) {
        for (char c : s.toCharArray()) if (Character.isUpperCase(c)) return true;
        return false;
    }

    private static boolean hasLowercase(String s) {
        for (char c : s.toCharArray()) if (Character.isLowerCase(c)) return true;
        return false;
    }

    private static boolean hasDigit(String s) {
        for (char c : s.toCharArray()) if (Character.isDigit(c)) return true;
        return false;
    }

    private static boolean hasSpecialChar(String s) {
        for (char c : s.toCharArray()) if (SPECIAL_CHARS.indexOf(c) >= 0) return true;
        return false;
    }
}
