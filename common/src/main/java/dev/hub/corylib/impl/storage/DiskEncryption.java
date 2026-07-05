package dev.hub.corylib.impl.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hub.corylib.api.entry.DataEntry;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

final class DiskEncryption {
    private static final String MARKER = "_corylib_encrypted";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private DiskEncryption() {
    }

    static boolean isEncrypted(JsonObject root) {
        return root.has(MARKER) && root.get(MARKER).getAsInt() == 1;
    }

    static <S, T> JsonObject encrypt(DataEntry<S, T> entry, JsonObject plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keyFor(entry), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.toString().getBytes(StandardCharsets.UTF_8));

            JsonObject root = new JsonObject();
            root.addProperty(MARKER, 1);
            root.addProperty("algorithm", ALGORITHM);
            root.addProperty("iv", Base64.getEncoder().encodeToString(iv));
            root.addProperty("payload", Base64.getEncoder().encodeToString(encrypted));
            return root;
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Failed to encrypt CoryLib entry '" + entry.id() + "'.", error);
        }
    }

    static <S, T> JsonObject decrypt(DataEntry<S, T> entry, JsonObject encryptedRoot) {
        try {
            String algorithm = encryptedRoot.get("algorithm").getAsString();
            if (!ALGORITHM.equals(algorithm)) {
                throw new IllegalStateException("Unsupported CoryLib encryption algorithm: " + algorithm);
            }
            byte[] iv = Base64.getDecoder().decode(encryptedRoot.get("iv").getAsString());
            byte[] payload = Base64.getDecoder().decode(encryptedRoot.get("payload").getAsString());
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keyFor(entry), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(payload);
            return JsonParser.parseString(new String(plain, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (GeneralSecurityException | RuntimeException error) {
            throw new IllegalStateException("Failed to decrypt CoryLib entry '" + entry.id() + "'.", error);
        }
    }

    private static <S, T> SecretKeySpec keyFor(DataEntry<S, T> entry) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("CoryLib disk encryption v1".getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(entry.modId().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(entry.key().getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest.digest(), "AES");
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("SHA-256 is required for CoryLib disk encryption.", error);
        }
    }
}
