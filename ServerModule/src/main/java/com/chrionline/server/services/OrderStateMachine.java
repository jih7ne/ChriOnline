package com.chrionline.server.services;
 
import com.chrionline.core.enums.StatutCommande;
import java.util.*;
 
/**
 * Machine à états pour les commandes e-commerce.
 *
 * Cycle de vie complet :
 *
 *   EN_ATTENTE ──► VALIDEE ──► EXPEDIEE ──► LIVREE
 *        │
 *        └──────────────────────────────────► ANNULEE
 *
 * Règles :
 *  - Une commande ne peut avancer que dans l'ordre défini.
 *  - LIVREE et ANNULEE sont des états terminaux (aucune transition possible).
 *  - Rester dans le même état est toujours autorisé (idempotence).
 */
public class OrderStateMachine {
 
    private static final Map<StatutCommande, Set<StatutCommande>> VALID_TRANSITIONS =
            new EnumMap<>(StatutCommande.class);
 
    static {
        // EN_ATTENTE : paiement OK → VALIDEE  |  annulation manuelle → ANNULEE
        VALID_TRANSITIONS.put(StatutCommande.EN_ATTENTE,
                EnumSet.of(StatutCommande.VALIDEE, StatutCommande.ANNULEE));

        // VALIDEE : expédition lancée → EXPEDIEE
        VALID_TRANSITIONS.put(StatutCommande.VALIDEE,
                EnumSet.of(StatutCommande.EXPEDIEE));

        // EXPEDIEE : livraison confirmée → LIVREE
        VALID_TRANSITIONS.put(StatutCommande.EXPEDIEE,
                EnumSet.of(StatutCommande.LIVREE));

        // LIVREE : état terminal — aucune transition possible
        VALID_TRANSITIONS.put(StatutCommande.LIVREE,
                EnumSet.noneOf(StatutCommande.class));

        // ANNULEE : état terminal — aucune transition possible
        VALID_TRANSITIONS.put(StatutCommande.ANNULEE,
                EnumSet.noneOf(StatutCommande.class));
    }
 
    /**
     * Vérifie si une transition entre deux statuts est autorisée.
     *
     * @param current le statut actuel de la commande
     * @param next    le nouveau statut demandé
     * @return {@code true} si la transition est légale, {@code false} sinon
     */
    public static boolean isValidTransition(StatutCommande current, StatutCommande next) {
        if (current == next) return true; // idempotence
        Set<StatutCommande> allowed = VALID_TRANSITIONS.get(current);
        return allowed != null && allowed.contains(next);
    }
 
    /**
     * Retourne l'ensemble des statuts accessibles depuis le statut courant.
     *
     * @param current le statut actuel
     * @return ensemble (potentiellement vide) des transitions autorisées
     */
    public static Set<StatutCommande> getNextValidStatuses(StatutCommande current) {
        return VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet());
    }

    /**
     * Indique si un statut est un état terminal (aucune transition sortante).
     *
     * @param statut le statut à tester
     * @return {@code true} si terminal
     */
    public static boolean isTerminal(StatutCommande statut) {
        Set<StatutCommande> next = VALID_TRANSITIONS.get(statut);
        return next != null && next.isEmpty();
    }

    /**
     * Libellé lisible du statut pour les notifications et logs.
     *
     * @param statut le statut de la commande
     * @return libellé en français
     */
    public static String describe(StatutCommande statut) {
        return switch (statut) {
            case EN_ATTENTE -> "En attente de paiement";
            case VALIDEE    -> "Commande validée";
            case EXPEDIEE   -> "Commande expédiée";
            case LIVREE     -> "Commande livrée";
            case ANNULEE    -> "Commande annulée";
        };
    }
}

