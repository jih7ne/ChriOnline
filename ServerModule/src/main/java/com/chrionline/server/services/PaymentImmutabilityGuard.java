package com.chrionline.server.services;

import com.chrionline.core.enums.StatutPaiement;
import com.chrionline.core.exceptions.BusinessException;
import com.chrionline.shared.models.Paiement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  GARDE D'IMMUABILITÉ DES PAIEMENTS CONFIRMÉS
 *
 * Règle métier fondamentale :
 * ─────────────────────────────────────────────
 *  UN PAIEMENT CONFIRMÉ NE PEUT JAMAIS ÊTRE MODIFIÉ OU SUPPRIMÉ.
 *
 * Pour des raisons d'audit financier et de conformité (PCI-DSS),
 * toute correction d'un paiement doit passer par la création d'un
 * NOUVEL enregistrement de remboursement (refund = new record).
 *
 * Responsabilités :
 *   1. Bloquer toute tentative de modification d'un paiement CONFIRME
 *   2. Bloquer les doubles paiements pour une même commande
 *   3. Loguer les tentatives de fraude/manipulation détectées
 *
 * Utilisation :
 *   PaymentImmutabilityGuard.assertNotConfirmed(paiement);   // lance BusinessException si confirmé
 *   PaymentImmutabilityGuard.assertNoDuplicatePayment(repo, idCommande);
 */
public class PaymentImmutabilityGuard {

    private static final Logger logger = LoggerFactory.getLogger(PaymentImmutabilityGuard.class);

    // ─── Vérification 1 : Paiement déjà confirmé → modification impossible ──

    /**
     * Lève une {@link BusinessException} si le paiement est déjà dans l'état CONFIRME.
     *
     * @param paiement le paiement à vérifier
     * @throws BusinessException si le paiement est confirmé (immuable)
     */
    public static void assertNotConfirmed(Paiement paiement) {
        if (paiement == null) {
            throw new BusinessException("Paiement introuvable.");
        }
        if (paiement.getStatut() == StatutPaiement.CONFIRME) {
            logger.warn(
                "🔒 IMMUABILITÉ: Tentative de modification d'un paiement CONFIRME bloquée " +
                "(id={}, commande={}). Créez un remboursement à la place.",
                paiement.getId(), paiement.getId_commande()
            );
            throw new BusinessException(
                "Impossible de modifier un paiement confirmé. " +
                "Pour rembourser un client, créez un nouvel enregistrement de remboursement."
            );
        }
    }

    // ─── Vérification 2 : Double paiement pour une même commande ────────────

    /**
     * Lève une {@link BusinessException} si un paiement confirmé existe déjà
     * pour la commande donnée (prévention du double paiement).
     *
     * @param existsByCommandeId résultat de {@code PaiementRepository.existsByCommandeId(idCommande)}
     * @param idCommande         identifiant de la commande vérifiée
     * @throws BusinessException si un paiement confirmé existe déjà
     */
    public static void assertNoDuplicatePayment(boolean existsByCommandeId, int idCommande) {
        if (existsByCommandeId) {
            logger.warn(
                "🔒 IMMUABILITÉ: Double paiement refusé pour commande id={}. " +
                "Un paiement CONFIRME existe déjà.",
                idCommande
            );
            throw new BusinessException(
                "Un paiement confirmé existe déjà pour la commande " + idCommande +
                ". Aucun double paiement autorisé."
            );
        }
    }

    // ─── Vérification 3 : Statut final → suppression impossible ─────────────

    /**
     * Lève une {@link BusinessException} si le paiement est dans un statut
     * qui ne permet aucune suppression (CONFIRME ou REMBOURSE).
     *
     * @param paiement le paiement à vérifier
     * @throws BusinessException si le paiement est dans un état final
     */
    public static void assertDeletable(Paiement paiement) {
        if (paiement == null) {
            throw new BusinessException("Paiement introuvable.");
        }
        if (paiement.getStatut() == StatutPaiement.CONFIRME) {
            logger.warn(
                "🔒 IMMUABILITÉ: Tentative de suppression d'un paiement CONFIRME bloquée " +
                "(id={}).",
                paiement.getId()
            );
            throw new BusinessException(
                "Impossible de supprimer un paiement confirmé. " +
                "L'historique financier doit rester intact."
            );
        }
    }
}
