package com.chrionline.core.enums;

/**
 * Énumération des rôles utilisateur dans le système ChriOnline.
 * Remplace les magic strings ("client", "admin") pour type-safety et maintenabilité.
 */
public enum UserRole {
    ADMIN("admin", "Administrateur - accès complet au système"),
    CLIENT("client", "Client - accès limité aux fonctionnalités client"),
    VENDOR("vendor", "Vendeur - gestion de catalogue réservée"),
    SUPPORT("support", "Support - accès aux notifications et tickets");

    private final String value;
    private final String description;

    UserRole(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Convertit une chaîne en énumération UserRole.
     * @param roleString la chaîne de rôle (ex: "admin", "client")
     * @return l'énumération correspondante, ou CLIENT par défaut
     */
    public static UserRole fromString(String roleString) {
        if (roleString == null || roleString.isBlank()) {
            return CLIENT;
        }
        for (UserRole role : UserRole.values()) {
            if (role.value.equalsIgnoreCase(roleString.trim())) {
                return role;
            }
        }
        return CLIENT; // Rôle par défaut pour sécurité
    }

    /**
     * Vérifie si le rôle a les permissions requises.
     * @param requiredRole le rôle requis
     * @return true si ce rôle a les permissions (hiérarchie)
     */
    public boolean hasPermission(UserRole requiredRole) {
        if (requiredRole == null) {
            return true; // Aucune restriction
        }
        // Hiérarchie : ADMIN > VENDOR/SUPPORT > CLIENT
        return this == ADMIN || this == requiredRole;
    }

    /**
     * @return true si ce rôle est administrateur
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * @return true si ce rôle est client standard
     */
    public boolean isClient() {
        return this == CLIENT;
    }

    @Override
    public String toString() {
        return value;
    }
}
