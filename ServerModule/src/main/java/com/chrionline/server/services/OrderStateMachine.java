package com.chrionline.server.services;
 
import com.chrionline.core.enums.StatutCommande;
import java.util.*;
 
/**
 * Machine à états pour les commandes.
 * Définit et valide les transitions autorisées entre les statuts.
 */
public class OrderStateMachine {
 
    private static final Map<StatutCommande, Set<StatutCommande>> VALID_TRANSITIONS = new EnumMap<>(StatutCommande.class);
 
    static {
        // EN_ATTENTE -> VALIDEE (paiement ok) ou ANNULEE
        VALID_TRANSITIONS.put(StatutCommande.EN_ATTENTE, EnumSet.of(StatutCommande.VALIDEE, StatutCommande.ANNULEE));
        
        // VALIDEE et ANNULEE sont des états finaux
        VALID_TRANSITIONS.put(StatutCommande.VALIDEE, EnumSet.noneOf(StatutCommande.class));
        VALID_TRANSITIONS.put(StatutCommande.ANNULEE, EnumSet.noneOf(StatutCommande.class));
    }
 
    /**
     * Vérifie si une transition entre deux statuts est autorisée.
     * 
     * @param current le statut actuel
     * @param next le nouveau statut demandé
     * @return true si la transition est valide, false sinon
     */
    public static boolean isValidTransition(StatutCommande current, StatutCommande next) {
        if (current == next) return true; // Rester dans le même état est autorisé
        
        Set<StatutCommande> allowed = VALID_TRANSITIONS.get(current);
        return allowed != null && allowed.contains(next);
    }
 
    /**
     * Retourne la liste des statuts accessibles depuis le statut actuel.
     */
    public static Set<StatutCommande> getNextValidStatuses(StatutCommande current) {
        return VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet());
    }
}
