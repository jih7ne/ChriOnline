package com.chrionline.core.validation;

import com.chrionline.core.exceptions.ValidationException;

import java.util.regex.Pattern;

/**
 * Framework de validation centralisé.
 * Centralise les règles de validation pour les emails, noms, montants, quantités, etc.
 */
public class AppValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    /**
     * Valide une adresse email.
     * @param email L'adresse email à valider.
     * @throws ValidationException si l'email est invalide ou null.
     */
    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("L'adresse email est requise.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Le format de l'adresse email est invalide.");
        }
        if (email.length() > 255) {
            throw new ValidationException("L'adresse email est trop longue (max 255 caractères).");
        }
    }

    /**
     * Valide un nom ou un prénom.
     * @param nom Le nom à valider.
     * @param fieldName Le nom du champ pour le message d'erreur (ex: "Le nom", "Le prénom").
     * @throws ValidationException si le nom est invalide.
     */
    public static void validateNom(String nom, String fieldName) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new ValidationException(fieldName + " est requis.");
        }
        if (nom.length() < 2 || nom.length() > 100) {
            throw new ValidationException(fieldName + " doit contenir entre 2 et 100 caractères.");
        }
    }

    /**
     * Valide un montant financier (ex: prix d'un produit).
     * @param montant Le montant à valider.
     * @throws ValidationException si le montant est invalide.
     */
    public static void validateMontant(Double montant) {
        if (montant == null) {
            throw new ValidationException("Le montant est requis.");
        }
        if (montant < 0) {
            throw new ValidationException("Le montant ne peut pas être négatif.");
        }
        if (montant > 1_000_000) {
            throw new ValidationException("Le montant est trop élevé.");
        }
    }

    /**
     * Valide une quantité (ex: stock, quantité commandée).
     * @param quantite La quantité à valider.
     * @throws ValidationException si la quantité est invalide.
     */
    public static void validateQuantite(Integer quantite) {
        if (quantite == null) {
            throw new ValidationException("La quantité est requise.");
        }
        if (quantite < 0) {
            throw new ValidationException("La quantité ne peut pas être négative.");
        }
        if (quantite > 10_000) {
            throw new ValidationException("La quantité dépasse la limite autorisée.");
        }
    }
}
