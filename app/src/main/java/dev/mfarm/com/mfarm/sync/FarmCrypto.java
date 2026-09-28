package dev.mfarm.com.mfarm.sync;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Backup files are encrypted with the farm join code so a Drive folder or
 * WhatsApp forward is not readable without the code.
 * Supports V2 authenticated encryption with V1 backward compatibility.
 */
public final class FarmCrypto {
    private static final byte[] MAGIC_V1 = new byte[]{'M', 'F', 'A', 'R', 'M', '1'};
    private static final byte[] MAGIC_V2 = new byte[]{'M', 'F', 'A', 'R', 'M', '2'};
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 16;
    private static final int HMAC_LEN = 32;
    private static final int V1_ITERATIONS = 10_000;
    private static final int V2_ITERATIONS = 50_000;
    private static final int KEY_BITS = 128;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final SecureRandom RANDOM = new SecureRandom();

    private FarmCrypto() {}

    public static byte[] encrypt(String plaintext, String joinCode) throws GeneralSecurityException {
        return encryptV2(plaintext, joinCode);
    }

    public static byte[] encryptV2(String plaintext, String joinCode) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(iv);

        SecretKey encKey = deriveKey(joinCode, salt, V2_ITERATIONS, "enc");
        SecretKey macKey = deriveKey(joinCode, salt, V2_ITERATIONS, "mac");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, encKey, new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(plaintext.getBytes(UTF8));

        byte[] mac = computeHmac(macKey, salt, iv, cipherText);

        byte[] out = new byte[MAGIC_V2.length + SALT_LEN + IV_LEN + HMAC_LEN + cipherText.length];
        System.arraycopy(MAGIC_V2, 0, out, 0, MAGIC_V2.length);
        System.arraycopy(salt, 0, out, MAGIC_V2.length, SALT_LEN);
        System.arraycopy(iv, 0, out, MAGIC_V2.length + SALT_LEN, IV_LEN);
        System.arraycopy(mac, 0, out, MAGIC_V2.length + SALT_LEN + IV_LEN, HMAC_LEN);
        System.arraycopy(cipherText, 0, out, MAGIC_V2.length + SALT_LEN + IV_LEN + HMAC_LEN, cipherText.length);
        return out;
    }

    public static String decrypt(byte[] blob, String joinCode) throws GeneralSecurityException {
        if (blob == null || blob.length < MAGIC_V1.length + SALT_LEN + IV_LEN + 16) {
            throw new GeneralSecurityException("Backup file is too small");
        }

        if (isMagicMatch(blob, MAGIC_V2)) {
            return decryptV2(blob, joinCode);
        } else if (isMagicMatch(blob, MAGIC_V1)) {
            return decryptV1(blob, joinCode);
        } else {
            throw new GeneralSecurityException("Not a valid MFarm backup format");
        }
    }

    private static String decryptV2(byte[] blob, String joinCode) throws GeneralSecurityException {
        if (blob.length < MAGIC_V2.length + SALT_LEN + IV_LEN + HMAC_LEN + 16) {
            throw new GeneralSecurityException("V2 Backup file is too small");
        }

        int offset = MAGIC_V2.length;
        byte[] salt = Arrays.copyOfRange(blob, offset, offset + SALT_LEN);
        offset += SALT_LEN;
        byte[] iv = Arrays.copyOfRange(blob, offset, offset + IV_LEN);
        offset += IV_LEN;
        byte[] expectedMac = Arrays.copyOfRange(blob, offset, offset + HMAC_LEN);
        offset += HMAC_LEN;
        byte[] cipherText = Arrays.copyOfRange(blob, offset, blob.length);

        SecretKey encKey = deriveKey(joinCode, salt, V2_ITERATIONS, "enc");
        SecretKey macKey = deriveKey(joinCode, salt, V2_ITERATIONS, "mac");

        byte[] computedMac = computeHmac(macKey, salt, iv, cipherText);
        if (!MessageDigest.isEqual(expectedMac, computedMac)) {
            throw new GeneralSecurityException("Backup file tampering detected or incorrect join code");
        }

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, encKey, new IvParameterSpec(iv));
        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, UTF8);
    }

    private static String decryptV1(byte[] blob, String joinCode) throws GeneralSecurityException {
        byte[] salt = Arrays.copyOfRange(blob, MAGIC_V1.length, MAGIC_V1.length + SALT_LEN);
        byte[] iv = Arrays.copyOfRange(blob, MAGIC_V1.length + SALT_LEN, MAGIC_V1.length + SALT_LEN + IV_LEN);
        byte[] cipherText = Arrays.copyOfRange(blob, MAGIC_V1.length + SALT_LEN + IV_LEN, blob.length);

        SecretKey key = deriveKey(joinCode, salt, V1_ITERATIONS, null);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, UTF8);
    }

    private static boolean isMagicMatch(byte[] blob, byte[] magic) {
        if (blob.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (blob[i] != magic[i]) return false;
        }
        return true;
    }

    private static byte[] computeHmac(SecretKey macKey, byte[] salt, byte[] iv, byte[] cipherText) throws GeneralSecurityException {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(macKey);
        hmac.update(salt);
        hmac.update(iv);
        return hmac.doFinal(cipherText);
    }

    private static SecretKey deriveKey(String joinCode, byte[] salt, int iterations, String purpose) throws GeneralSecurityException {
        String password = FarmIdentity.normalizeJoinCode(joinCode);
        if (password.isEmpty()) {
            throw new GeneralSecurityException("Join code required");
        }
        if (purpose != null) {
            password = password + ":" + purpose;
        }
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        byte[] encoded = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(encoded, "AES");
    }
}
