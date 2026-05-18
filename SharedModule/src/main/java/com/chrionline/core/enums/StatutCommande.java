package com.chrionline.core.enums;

/**
 * Cycle de vie complet d'une commande.
 *
 * Transitions autorisées (voir OrderStateMachine) :
 *   EN_ATTENTE  → VALIDEE | ANNULEE
 *   VALIDEE     → EN_PREPARATION | ANNULEE
 *   EN_PREPARATION → EXPEDIEE
 *   EXPEDIEE    → LIVREE
 *   LIVREE      → (état final)
 *   ANNULEE     → (état final)
 */
public enum StatutCommande {
    EN_ATTENTE,
    VALIDEE,
    EN_PREPARATION,
    EXPEDIEE,
    LIVREE,
    ANNULEE
}

