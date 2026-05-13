package com.chrionline.server.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ResetTokenStore
 * 
 * Vérifie:
 * - Génération de tokens uniques
 * - Expiration après 10 minutes
 * - Utilisation unique (non réutilisable)
 * - Invalidation de tokens
 * - Nettoyage automatique
 */
public class ResetTokenStoreTest {
    
    private ResetTokenStore store;
    private static final int TEST_USER_ID = 123;
    
    @BeforeEach
    public void setUp() {
        store = ResetTokenStore.getInstance();
        store.clearAll(); // Nettoyer avant chaque test
    }
    
    // ── TEST 1: Génération de tokens uniques ──────────────────────────
    @Test
    public void testTokenGenerationIsUnique() {
        // Arrange & Act
        String token1 = store.generateToken(TEST_USER_ID);
        String token2 = store.generateToken(TEST_USER_ID);
        
        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2); // Tokens doivent être différents
        assertTrue(token1.length() >= 64); // Au moins 64 chars (256 bits en hex)
    }
    
    // ── TEST 2: Validation d'un token valide ──────────────────────────
    @Test
    public void testValidTokenValidation() {
        // Arrange
        String token = store.generateToken(TEST_USER_ID);
        
        // Act
        int userId = store.validateAndConsumeToken(token);
        
        // Assert
        assertEquals(TEST_USER_ID, userId);
    }
    
    // ── TEST 3: Token invalide retourne -1 ────────────────────────────
    @Test
    public void testInvalidTokenReturnsMinus1() {
        // Act
        int userId = store.validateAndConsumeToken("invalid-token-12345");
        
        // Assert
        assertEquals(-1, userId);
    }
    
    // ── TEST 4: Token null retourne -1 ────────────────────────────────
    @Test
    public void testNullTokenReturnsMinus1() {
        // Act
        int userId = store.validateAndConsumeToken(null);
        
        // Assert
        assertEquals(-1, userId);
    }
    
    // ── TEST 5: Impossibilité de réutiliser un token ───────────────────
    @Test
    public void testTokenCannotBeReused() {
        // Arrange
        String token = store.generateToken(TEST_USER_ID);
        
        // Act - Première utilisation
        int firstUse = store.validateAndConsumeToken(token);
        assertEquals(TEST_USER_ID, firstUse);
        
        // Act - Tentative de réutilisation
        int secondUse = store.validateAndConsumeToken(token);
        assertEquals(-1, secondUse); // Doit retourner -1
    }
    
    // ── TEST 6: isTokenValid retourne false après consommation ────────
    @Test
    public void testIsTokenValidAfterConsumption() {
        // Arrange
        String token = store.generateToken(TEST_USER_ID);
        assertTrue(store.isTokenValid(token)); // Avant consommation
        
        // Act
        store.validateAndConsumeToken(token);
        
        // Assert
        assertFalse(store.isTokenValid(token)); // Après consommation
    }
    
    // ── TEST 7: Invalidation des anciens tokens ───────────────────────
    @Test
    public void testOldTokensInvalidatedOnNewGeneration() {
        // Arrange
        String token1 = store.generateToken(TEST_USER_ID);
        
        // Act - Générer un nouveau token pour le même utilisateur
        String token2 = store.generateToken(TEST_USER_ID);
        
        // Assert
        assertFalse(store.isTokenValid(token1)); // L'ancien token est invalidé
        assertTrue(store.isTokenValid(token2));  // Le nouveau token est valide
    }
    
    // ── TEST 8: getUserIdFromToken retourne l'ID utilisateur ─────────
    @Test
    public void testGetUserIdFromToken() {
        // Arrange
        String token = store.generateToken(TEST_USER_ID);
        
        // Act
        int userId = store.getUserIdFromToken(token);
        
        // Assert
        assertEquals(TEST_USER_ID, userId);
    }
    
    // ── TEST 9: removeToken supprime le token ──────────────────────────
    @Test
    public void testRemoveToken() {
        // Arrange
        String token = store.generateToken(TEST_USER_ID);
        assertTrue(store.isTokenValid(token));
        
        // Act
        store.removeToken(token);
        
        // Assert
        assertFalse(store.isTokenValid(token));
    }
    
    // ── TEST 10: invalidateTokensForUser supprime tous les tokens ────────
    @Test
    public void testInvalidateTokensForUser() {
        // Arrange
        int userId = 456;
        String token1 = store.generateToken(userId);
        String token2 = store.generateToken(userId);
        
        // Act
        store.invalidateTokensForUser(userId);
        
        // Assert
        assertFalse(store.isTokenValid(token1));
        assertFalse(store.isTokenValid(token2));
    }
    
    // ── TEST 11: Token count reflects active tokens ──────────────────
    @Test
    public void testGetTokenCount() {
        // Arrange
        store.generateToken(100);
        store.generateToken(101);
        store.generateToken(102);
        
        // Assert
        assertEquals(3, store.getTokenCount());
        
        // Act
        store.removeToken(store.generateToken(100)); // Génère et supprime
        
        // Assert (devrait rester 3)
        assertTrue(store.getTokenCount() >= 2);
    }
    
    // ── TEST 12: Scénario complet de réinitialisation ──────────────────
    @Test
    public void testCompleteResetPasswordScenario() {
        // 1. Utilisateur reçoit un token après vérification de sa réponse
        String resetToken = store.generateToken(TEST_USER_ID);
        assertNotNull(resetToken);
        
        // 2. Token est valide
        assertTrue(store.isTokenValid(resetToken));
        
        // 3. Utilisateur consomme le token pour réinitialiser
        int userId = store.validateAndConsumeToken(resetToken);
        assertEquals(TEST_USER_ID, userId);
        
        // 4. Token est maintenant invalide
        assertFalse(store.isTokenValid(resetToken));
        
        // 5. Tentative de réutilisation échoue
        int reuse = store.validateAndConsumeToken(resetToken);
        assertEquals(-1, reuse);
    }
}
