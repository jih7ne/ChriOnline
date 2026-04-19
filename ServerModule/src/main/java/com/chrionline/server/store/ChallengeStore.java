package com.chrionline.server.store;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ChallengeStore {

    private static final String FILE_PATH = "challenges/challenges.dat";



    public static void save(String id, String challenge, long expiresAt) throws IOException {
        Map<String, ChallengeEntry> store = loadAll();
        store.put(id, new ChallengeEntry(challenge, expiresAt));
        persist(store);
    }



    public static Optional<ChallengeEntry> find(String id) throws IOException {
        return Optional.ofNullable(loadAll().get(id));
    }



    public static void delete(String id) throws IOException {
        Map<String, ChallengeEntry> store = loadAll();
        store.remove(id);
        persist(store);
    }

    public static void purgeExpired() throws IOException {
        long now = System.currentTimeMillis();
        Map<String, ChallengeEntry> store = loadAll();
        store.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
        persist(store);
    }



    @SuppressWarnings("unchecked")
    private static Map<String, ChallengeEntry> loadAll() throws IOException {
        Path path = Paths.get(FILE_PATH);
        if (!Files.exists(path)) return new HashMap<>();

        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            return (Map<String, ChallengeEntry>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Corrupted challenge store.", e);
        }
    }

    private static void persist(Map<String, ChallengeEntry> store) throws IOException {
        Path path = Paths.get(FILE_PATH);
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path)))) {
            out.writeObject(store);
        }
    }



    public record ChallengeEntry(String challenge, long expiresAt) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
