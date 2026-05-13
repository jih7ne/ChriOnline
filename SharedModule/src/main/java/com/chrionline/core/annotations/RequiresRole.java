package com.chrionline.core.annotations;

import com.chrionline.core.enums.UserRole;

import java.lang.annotation.*;

/**
 * Annotation pour restriction d'accès basée sur les rôles (RBAC).
 * 
 * Utilisée sur les méthodes des contrôleurs pour indiquer quels rôles
 * peuvent accéder à l'endpoint. Le RequestDispatcher vérifie cette
 * annotation avant d'invoquer la méthode.
 *
 * Exemples:
 *  @RequiresRole(UserRole.ADMIN)
 *  public String deleteUser(AppRequest req) { ... }
 *
 *  @RequiresRole({UserRole.ADMIN, UserRole.SUPPORT})
 *  public String viewLogs(AppRequest req) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    /**
     * Tableau des rôles autorisés pour accéder à cette méthode.
     * Si vide ou non spécifié, seul ADMIN est autorisé.
     * 
     * @return tableau de rôles requis
     */
    UserRole[] value() default {UserRole.ADMIN};

    /**
     * Description optionnelle de la restriction pour la documentation.
     * Exemple: "Seuls les administrateurs peuvent supprimer des produits"
     * 
     * @return description de la restriction
     */
    String description() default "";

    /**
     * Si true, les utilisateurs non-authentifiés sont rejetés.
     * Si false, un utilisateur peut être null (attention: utiliser avec modération).
     * 
     * @return true si l'authentification est requise
     */
    boolean requiresAuthentication() default true;
}
