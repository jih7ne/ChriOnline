package com.chrionline.core.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de limitation de taux (Rate Limiting) par IP ou par Utilisateur.
 * Utilise un algorithme simplifié (Fixed Window ou Token Bucket simplifié).
 */
public class RateLimiter {

    // Limite: 100 requêtes par fenêtre de temps
    private static final int MAX_REQUESTS = 100;
    // Fenêtre de temps: 60 secondes
    private static final long TIME_WINDOW_MS = 60_000;

    private static final ConcurrentHashMap<String, RequestData> ipRequests = new ConcurrentHashMap<>();

    private static class RequestData {
        int count;
        long windowStart;

        RequestData(long startTime) {
            this.count = 1;
            this.windowStart = startTime;
        }
    }

    /**
     * Vérifie si l'IP a dépassé la limite de requêtes.
     * @param ip L'adresse IP à vérifier
     * @return true si la requête est autorisée, false si elle est bloquée (rate limit)
     */
    public static boolean allowRequest(String ip) {
        if (ip == null) return true;

        long now = System.currentTimeMillis();
        
        return ipRequests.compute(ip, (key, data) -> {
            if (data == null || (now - data.windowStart > TIME_WINDOW_MS)) {
                // Nouvelle fenêtre
                return new RequestData(now);
            } else {
                data.count++;
                return data;
            }
        }).count <= MAX_REQUESTS;
    }
}
