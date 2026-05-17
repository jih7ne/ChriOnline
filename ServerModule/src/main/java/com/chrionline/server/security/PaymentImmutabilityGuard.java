package com.chrionline.server.security;

import com.chrionline.core.enums.StatutPaiement;
import com.chrionline.core.exceptions.BusinessException;
import com.chrionline.shared.models.Paiement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Garde de sécurité pour l'immuabilité des paiements confirmés.
 *
 * Règle métier fondamentale :
 *   Un paiement CONFIRME ne peut JAMAIS être modifié.
 *   Toute correction financière (remboursement) doit être un NOUVEL enregistrement
 *   en base de données avec un statut dédié (ex: REMBOURSE).
 *
 * Cette classe encapsule ce contrôle de manière réutilisable et testable,
 * séparément du PaiementService.
 */
public class PaymentImmutabilityGuard {

    private static final Logger logger = LoggerFactory.getLogger(PaymentImmutabilityGuard.class);

    /**
     * Vérifie qu'un paiement peut être modifié.
     * Lance une {@link BusinessException} si le paiement est confirmé.
     *
     * @param paiement le paiement à contrôler
     * @throws BusinessException si le paiement est dans un état confirmé
     */
    public static void assertMutable(Paiement paiement) {
        if (paiement == null) {
            throw new BusinessException("Paiement introuvable ou accès non autorisé.");
        }
        if (paiement.getStatut() == StatutPaiement.CONFIRME) {
            logger.warn(
                "🔒 IMMUTABILITY GUARD: Tentative de modification d'un paiement CONFIRME " +
                "(id={}, commande={}) bloquée.",
                paiement.getId(), paiement.getId_commande()
            );
            throw new BusinessException(
                "Impossible de modifier un paiement confirmé. " +
                "Veuillez créer un remboursement (nouvel enregistrement)."
            );
        }
    }

    /**
     * Vérifie qu'un paiement peut être modifié (version boolean, sans exception).
     *
     * @param paiement le paiement à contrôler
     * @return {@code true} si le paiement est modifiable, {@code false} s'il est confirmé
     */
    public static boolean isMutable(Paiement paiement) {
        if (paiement == null) return false;
        return paiement.getStatut() != StatutPaiement.CONFIRME;
    }

    /**
     * Vérifie qu'aucun paiement confirmé n'existe déjà pour une commande donnée,
     * afin d'éviter les doubles paiements.
     *
     * Usage : appeler avant d'insérer un nouveau paiement.
     *
     * @param commandeId    identifiant de la commande
     * @param alreadyPaid   résultat du repository (existsByCommandeId)
     * @throws BusinessException si un paiement confirmé existe déjà
     */
    public static void assertNoDuplicatePayment(int commandeId, boolean alreadyPaid) {
        if (alreadyPaid) {
            logger.warn(
                "🔒 IMMUTABILITY GUARD: Double paiement refusé pour commande id={}.",
                commandeId
            );
            throw new BusinessException(
                "Un paiement confirmé existe déjà pour la commande " + commandeId + "."
            );
        }
    }
}
