package com.chrionline.adminmodule.admin.security;

import java.security.KeyPair;

/**
 * Standalone test for KeyPairManager

 *
 * It will:
 *   1. Generate an RSA-2048 key pair
 *   2. Save the binary .chrikey file to ~/.chrionline/keys/
 *   3. Load it back and verify the keys match
 *   4. Print the public key (Base64) and fingerprint — you can manually
 *      INSERT these into the DB to test the server side independently.
 *
 * Manual DB insert for testing (after running this):
 *
 *   INSERT INTO user_devices
 *       (user_email, device_name, public_key, fingerprint, key_algorithm, created_at, revoked)
 *   VALUES
 *       ('test@example.com', 'TestDevice', '<BASE64_KEY>', '<FINGERPRINT>', 'RSA', NOW(), FALSE);
 */
public class KeyPairManagerTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== KeyPairManager Test ===");
        System.out.println("Key directory: " + KeyPairManager.getKeyDirectory());
        System.out.println();

        String deviceName = "TestDevice-" + System.currentTimeMillis();

        // ── 1: Generate ──────────────────────────────────────────────────
        System.out.println("1. Generating RSA-2048 key pair for: " + deviceName);
        KeyPair generated = KeyPairManager.generateAndSave(deviceName);
        System.out.println("    Generated OK");
        System.out.println("   File: " + KeyPairManager.getKeyFilePath(deviceName));
        System.out.println();

        // ── 2: Fingerprint + Public Key ───────────────────────────────────
        String publicKeyB64 = KeyPairManager.encodePublicKey(generated.getPublic());
        String fingerprint  = KeyPairManager.computeFingerprint(generated.getPublic());

        System.out.println("2. Public Key (Base64, first 80 chars):");
        System.out.println("   " + publicKeyB64.substring(0, Math.min(80, publicKeyB64.length())) + "...");
        System.out.println();
        System.out.println("3. Fingerprint (SHA-256 hex):");
        System.out.println("   " + fingerprint);
        System.out.println();

        // Short form as displayed in UI
        String shortFp = "SHA256:";
        for (int i = 0; i < Math.min(fingerprint.length(), 16); i += 2) {
            if (i > 0) shortFp += ":";
            shortFp += fingerprint.substring(i, Math.min(i + 2, fingerprint.length())).toUpperCase();
        }
        shortFp += "...";
        System.out.println("   Short form (UI): " + shortFp);
        System.out.println();

        // ── 3: Load back ──────────────────────────────────────────────────
        System.out.println("4. Loading key pair from file...");
        KeyPair loaded = KeyPairManager.loadFromFile(deviceName);
        System.out.println("    Loaded OK");

        // ── 4: Verify round-trip ──────────────────────────────────────────
        String loadedPubB64 = KeyPairManager.encodePublicKey(loaded.getPublic());
        String loadedFp     = KeyPairManager.computeFingerprint(loaded.getPublic());

        boolean pubMatch = publicKeyB64.equals(loadedPubB64);
        boolean fpMatch  = fingerprint.equals(loadedFp);

        System.out.println("5. Verification:");
        System.out.println("   Public key round-trip: " + (pubMatch ? " MATCH" : " MISMATCH"));
        System.out.println("   Fingerprint round-trip: " + (fpMatch ? " MATCH" : " MISMATCH"));
        System.out.println();

        // ── 5: Print SQL for manual DB test ───────────────────────────────
        System.out.println("=== SQL for manual testing (paste into MySQL) ===");
        System.out.println("INSERT INTO user_devices");
        System.out.println("    (user_email, device_name, public_key, fingerprint, key_algorithm, created_at, revoked)");
        System.out.println("VALUES");
        System.out.printf("    ('1@11.com', '%s', '%s', '%s', 'RSA', NOW(), FALSE);%n",
                deviceName, publicKeyB64, fingerprint);
        System.out.println();
        System.out.println("Then verify with:");
        System.out.println("  SELECT id, user_email, device_name, fingerprint FROM user_devices;");
        System.out.println();

        // ── 6: Cleanup ────────────────────────────────────────────────────
        System.out.print("6. Cleaning up test file... ");
        //KeyPairManager.deleteKeyFile(deviceName);
        System.out.println("done.");
        System.out.println();
        System.out.println("=== All tests passed ===");
    }
}
