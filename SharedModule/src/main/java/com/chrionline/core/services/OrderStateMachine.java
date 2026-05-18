package com.chrionline.core.services;

import com.chrionline.core.enums.StatutCommande;
import java.util.*;

/**
 * Machine à états pour le cycle de vie complet des commandes.
 * Placée dans SharedModule pour être accessible par ServerModule ET AdminModule.
 *
 * Transitions autorisées :
 *
 *   EN_ATTENTE     →  VALIDEE         (paiement accepté)
 *   EN_ATTENTE     →  ANNULEE         (annulation avant paiement)
 *
 *   VALIDEE        →  EN_PREPARATION  (admin lance la préparation)
 *   VALIDEE        →  ANNULEE         (remboursement admin)
 *
 *   EN_PREPARATION →  EXPEDIEE        (colis remis au transporteur)
 *
 *   EXPEDIEE       →  LIVREE          (livraison confirmée)
 *
 *   LIVREE         →  (état final — aucune transition)
 *   ANNULEE        →  (état final — aucune transition)
 */
public class OrderStateMachine {

    private static final Map<StatutCommande, Set<StatutCommande>> VALID_TRANSITIONS =
            new EnumMap<>(StatutCommande.class);

    static {
        VALID_TRANSITIONS.put(StatutCommande.EN_ATTENTE,
                EnumSet.of(StatutCommande.VALIDEE, StatutCommande.ANNULEE));

        VALID_TRANSITIONS.put(StatutCommande.VALIDEE,
                EnumSet.of(StatutCommande.EN_PREPARATION, StatutCommande.ANNULEE));

        VALID_TRANSITIONS.put(StatutCommande.EN_PREPARATION,
                EnumSet.of(StatutCommande.EXPEDIEE));

        VALID_TRANSITIONS.put(StatutCommande.EXPEDIEE,
                EnumSet.of(StatutCommande.LIVREE));

        VALID_TRANSITIONS.put(StatutCommande.LIVREE,
                EnumSet.noneOf(StatutCommande.class));

        VALID_TRANSITIONS.put(StatutCommande.ANNULEE,
                EnumSet.noneOf(StatutCommande.class));
    }

    /**
     * Vérifie si une transition entre deux statuts est autorisée.
     */
    public static boolean isValidTransition(StatutCommande current, StatutCommande next) {
        if (current == next) return true;
        Set<StatutCommande> allowed = VALID_TRANSITIONS.get(current);
        return allowed != null && allowed.contains(next);
    }

    /**
     * Retourne la liste des statuts accessibles depuis le statut actuel.
     */
    public static Set<StatutCommande> getNextValidStatuses(StatutCommande current) {
        return VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet());
    }

    /**
     * Indique si un statut est un état final (aucune transition possible).
     */
    public static boolean isFinalState(StatutCommande statut) {
        Set<StatutCommande> next = VALID_TRANSITIONS.get(statut);
        return next != null && next.isEmpty();
    }
}
